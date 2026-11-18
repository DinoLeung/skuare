// Translation checks, run by `./gradlew check`. See the Translation section of CLAUDE.md.
//
// Applied by composeApp/build.gradle.kts. It lives here rather than next to it because Android lint
// reads every Gradle script in the module and crashes on an applied one. The script compiler also
// crashes on a class declared inside a task, hence the local functions.

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/*
 * checkUiStrings fails on text written straight into the UI instead of coming from a string
 * resource: Text("…"), ErrorText("…"), Hint("…") and text, label, title, … = "…" with a letter in
 * it. Previews are exempt, as is a line ending in `// i18n-ignore` (e.g. a brand name).
 */
tasks.register("checkUiStrings") {
	group = "verification"
	description = "Finds hard-coded UI text that should be a string resource."
	val sources = fileTree("src") { include("*Main/kotlin/**/ui/**/*.kt") }
	val projectDir = layout.projectDirectory.asFile
	inputs.files(sources)

	doLast {
		val literalArgument = Regex("""\b(Text|ErrorText|Hint)\(\s*"[^"]*\p{L}""")
		val literalNamed = Regex(
			"""\b(text|label|title|contentDescription|placeholder|supportingText|allSelectedText|noneSelectedText)\s*=\s*"[^"]*\p{L}"""
		)
		val problems = sources.files.sorted().flatMap { file ->
			var depth = 0
			var previewPending = false
			// the depth a preview's body closes at, null outside previews
			var previewEndDepth: Int? = null
			file.readLines().mapIndexedNotNull { index, line ->
				val code = line.trim()
				if (code.startsWith("@Preview")) previewPending = true
				val exempt = previewPending || previewEndDepth != null ||
					code.startsWith("//") || code.startsWith("*") || line.trimEnd().endsWith("// i18n-ignore")
				val hardCoded = literalArgument.containsMatchIn(line) || literalNamed.containsMatchIn(line)
				val problem = if (!exempt && hardCoded) {
					"${file.relativeTo(projectDir)}:${index + 1}: hard-coded UI text, use a string resource: $code"
				} else null
				line.forEach { char ->
					when (char) {
						'{' -> {
							if (previewPending) {
								previewEndDepth = depth
								previewPending = false
							}
							depth++
						}

						'}' -> {
							depth--
							if (depth == previewEndDepth) previewEndDepth = null
						}
					}
				}
				problem
			}
		}
		if (problems.isNotEmpty()) {
			throw GradleException(
				"Hard-coded UI text, see the Translation section of CLAUDE.md. End a line with " +
					"// i18n-ignore if it must not be translated.\n" + problems.joinToString("\n")
			)
		}
	}
}

/*
 * checkTranslations fails on translations that would break at runtime or show the wrong thing,
 * and on language lists that disagree:
 * - a translated key that isn't in values/, or that values/ marks translatable="false"
 * - a placeholder that isn't positional (%1$s, %1$d), the only kind Compose resources fill in
 * - a translated string whose placeholders differ from the English one, or a plural item with a
 *   placeholder the English plural doesn't have
 * - \' in a composeResources string, which Compose resources show as is
 * - a language with composeResources but missing from res/xml/locales_config.xml, from
 *   CFBundleLocalizations in iosApp/iosApp/Info.plist or from Android's res/values-<lang>, or the
 *   other way round
 */
tasks.register("checkTranslations") {
	group = "verification"
	description = "Checks translated strings against English and the declared languages."
	val composeResources = layout.projectDirectory.dir("src/commonMain/composeResources")
	val androidResources = layout.projectDirectory.dir("src/androidMain/res")
	val infoPlist = rootProject.layout.projectDirectory.file("iosApp/iosApp/Info.plist")
	inputs.dir(composeResources)
	inputs.dir(androidResources)
	inputs.file(infoPlist)

	doLast {
		val problems = mutableListOf<String>()
		val positional = Regex("""%\d+\$[ds]""")
		// values-de, values-pt-rBR
		val languageFolder = Regex("""values-([a-z]{2,3}(?:-r[A-Z]{2})?)""")

		fun parse(file: File): Element =
			DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement

		fun Element.children(tag: String): List<Element> {
			val nodes = getElementsByTagName(tag)
			return (0 until nodes.length).map { nodes.item(it) as Element }
		}

		// key to its text, or its items' texts for a plural
		fun texts(file: File): Map<String, List<String>> {
			val root = parse(file)
			return root.children("string").associate { it.getAttribute("name") to listOf(it.textContent) } +
				root.children("plurals").associate { plural ->
					plural.getAttribute("name") to plural.children("item").map { it.textContent }
				}
		}

		fun pluralKeys(file: File) = parse(file).children("plurals").map { it.getAttribute("name") }.toSet()

		fun untranslatableKeys(file: File) =
			parse(file).children("string").filter { it.getAttribute("translatable") == "false" }
				.map { it.getAttribute("name") }.toSet()

		fun placeholders(text: String) = positional.findAll(text).map { it.value }.toSet()

		fun checkStyle(file: File) = texts(file).forEach { (key, values) ->
			values.forEach { text ->
				if (text.count { it == '%' } != positional.findAll(text).count()) {
					problems += "$file: $key: use positional placeholders (%1\$s, %1\$d), got \"$text\""
				}
				if (text.contains("\\'")) problems += "$file: $key: write apostrophes as ', Compose resources show \\' as is"
			}
		}

		fun languagesOf(resourceDir: File) = resourceDir.listFiles().orEmpty()
			.mapNotNull { languageFolder.matchEntire(it.name)?.groupValues?.get(1)?.replace("-r", "-") }
			.toSet()

		// Compose resources: every values-<lang> file against the values/ file of the same name
		val baseDir = composeResources.dir("values").asFile
		baseDir.listFiles { file -> file.extension == "xml" }.orEmpty().forEach(::checkStyle)
		composeResources.asFile.listFiles().orEmpty().filter { languageFolder.matches(it.name) }.forEach { dir ->
			dir.listFiles { file -> file.extension == "xml" }.orEmpty().forEach translations@{ file ->
				val baseFile = File(baseDir, file.name)
				if (!baseFile.exists()) {
					problems += "$file: no values/${file.name} to translate"
					return@translations
				}
				checkStyle(file)
				val english = texts(baseFile)
				val englishPlurals = pluralKeys(baseFile)
				val untranslatable = untranslatableKeys(baseFile)
				val plurals = pluralKeys(file)
				texts(file).forEach { (key, values) ->
					val englishValues = english[key]
					when {
						englishValues == null -> problems += "$file: $key isn't in values/${file.name}"
						key in untranslatable -> problems += "$file: $key is translatable=\"false\""
						(key in plurals) != (key in englishPlurals) ->
							problems += "$file: $key must be a ${if (key in englishPlurals) "plural" else "string"}"

						key in englishPlurals -> {
							val allowed = englishValues.flatMap(::placeholders).toSet()
							values.forEach { text ->
								val extra = placeholders(text) - allowed
								if (extra.isNotEmpty()) problems += "$file: $key has $extra the English plural doesn't"
							}
						}

						placeholders(values.single()) != placeholders(englishValues.single()) ->
							problems += "$file: $key has placeholders ${placeholders(values.single())}, " +
								"English has ${placeholders(englishValues.single())}"
					}
				}
			}
		}

		// declared languages, English being the base everywhere
		val composeLanguages = languagesOf(composeResources.asFile)
		val localeConfig = parse(androidResources.file("xml/locales_config.xml").asFile)
			.children("locale").map { it.getAttribute("android:name") }.toSet() - "en"
		val iosLanguages = parse(infoPlist.asFile).children("key")
			.firstOrNull { it.textContent == "CFBundleLocalizations" }
			?.let { key ->
				var sibling = key.nextSibling
				while (sibling != null && sibling !is Element) sibling = sibling.nextSibling
				(sibling as? Element)?.children("string")?.map { it.textContent }
			}.orEmpty().toSet() - "en"
		mapOf(
			"res/xml/locales_config.xml" to localeConfig,
			"CFBundleLocalizations in Info.plist" to iosLanguages,
			"Android res/values-<lang>" to languagesOf(androidResources.asFile),
		).forEach { (where, languages) ->
			(composeLanguages - languages).forEach { problems += "$it has composeResources but is missing from $where" }
			(languages - composeLanguages).forEach { problems += "$it is in $where but has no composeResources/values-$it" }
		}

		if (problems.isNotEmpty()) {
			throw GradleException(
				"Translation problems, see the Translation section of CLAUDE.md:\n" + problems.joinToString("\n")
			)
		}
	}
}

tasks.named("check") {
	dependsOn("checkUiStrings", "checkTranslations")
}
