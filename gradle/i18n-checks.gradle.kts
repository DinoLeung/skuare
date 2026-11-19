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
 * it, ordinary or triple-quoted, also when the call spans lines. Comments (nested ones too) and
 * previews are exempt, as is a literal on a line ending in `// i18n-ignore` (e.g. a brand name).
 */
tasks.register("checkUiStrings") {
	group = "verification"
	description = "Finds hard-coded UI text that should be a string resource."
	val sources = fileTree("src") { include("*Main/kotlin/**/ui/**/*.kt") }
	val projectDir = layout.projectDirectory.asFile
	inputs.files(sources)

	doLast {
		// a whole string literal, triple-quoted or ordinary with escapes, e.g. "Say \"hi\""
		val literal = """("{3}[\s\S]*?"{3}|"(?:\\.|[^"\\\n])*")"""
		val hardCoded = Regex(
			"""\b(?:Text|ErrorText|Hint)\(\s*$literal""" +
				"""|\b(?:text|label|title|contentDescription|placeholder|supportingText|allSelectedText|noneSelectedText)\s*=\s*$literal"""
		)

		// The source with comments blanked out (line breaks kept), and the ranges of preview
		// functions, whose braces are counted outside strings and character literals. String
		// templates with quotes of their own aren't followed, which is close enough for UI code.
		fun scan(source: String): Pair<String, List<IntRange>> {
			val code = StringBuilder(source.length)
			val previews = mutableListOf<IntRange>()
			var depth = 0
			var inString = false
			var inRawString = false
			var previewStart = -1
			var previewDepth: Int? = null
			var i = 0
			while (i < source.length) {
				val char = source[i]
				when {
					inRawString -> {
						if (source.startsWith("\"\"\"", i)) {
							code.append("\"\"\"")
							i += 3
							inRawString = false
							continue
						}
						code.append(char)
					}

					inString -> {
						if (char == '\\' && i + 1 < source.length) {
							code.append(char).append(source[i + 1])
							i += 2
							continue
						}
						if (char == '"' || char == '\n') inString = false
						code.append(char)
					}

					source.startsWith("//", i) -> {
						val end = source.indexOf('\n', i).let { if (it < 0) source.length else it }
						repeat(end - i) { code.append(' ') }
						i = end
						continue
					}

					source.startsWith("/*", i) -> {
						// Kotlin block comments nest, the comment ends where its own opener closes
						var nesting = 0
						var end = i
						while (end < source.length) {
							when {
								source.startsWith("/*", end) -> {
									nesting++
									end += 2
								}

								source.startsWith("*/", end) -> {
									nesting--
									end += 2
									if (nesting == 0) break
								}

								else -> end++
							}
						}
						for (k in i until end) code.append(if (source[k] == '\n') '\n' else ' ')
						i = end
						continue
					}

					source.startsWith("\"\"\"", i) -> {
						code.append("\"\"\"")
						i += 3
						inRawString = true
						continue
					}

					char == '"' -> {
						inString = true
						code.append(char)
					}

					char == '\'' -> {
						// a character literal, e.g. '{' or '\''
						val end = source.indexOf('\'', if (source.getOrNull(i + 1) == '\\') i + 3 else i + 2)
						val last = if (end < 0) i else end
						code.append(source, i, last + 1)
						i = last + 1
						continue
					}

					char == '{' -> {
						if (previewStart >= 0 && previewDepth == null) previewDepth = depth
						depth++
						code.append(char)
					}

					char == '}' -> {
						depth--
						code.append(char)
						if (previewDepth != null && depth == previewDepth) {
							previews += previewStart..i
							previewStart = -1
							previewDepth = null
						}
					}

					else -> {
						if (previewStart < 0 && source.startsWith("@Preview", i)) previewStart = i
						code.append(char)
					}
				}
				i++
			}
			return code.toString() to previews
		}

		val problems = sources.files.sorted().flatMap { file ->
			val source = file.readText()
			val lines = source.lines()
			val (code, previews) = scan(source)
			// what a literal shows: a raw string as written, an ordinary one with its escapes
			// decoded, so the letters of e.g. \n or \t don't count
			fun content(literal: String): String =
				if (literal.startsWith("\"\"\"")) literal.removeSurrounding("\"\"\"")
				else literal.removeSurrounding("\"").replace(Regex("""\\u([0-9a-fA-F]{4})|\\(.)""")) { escape ->
					escape.groups[1]?.let { it.value.toInt(16).toChar().toString() }
						?: if (escape.groupValues[2] in setOf("n", "t", "r", "b")) " " else escape.groupValues[2]
				}
			hardCoded.findAll(code).mapNotNull { match ->
				// the line of the literal's opening quote, the call may start on an earlier one
				val quote = match.range.first + match.value.indexOf('"')
				val line = code.substring(0, quote).count { it == '\n' }
				// e.g. "", "00:00" or "\n" has nothing to translate
				val literal = (match.groups[1] ?: match.groups[2])!!.value
				val exempt = content(literal).none { it.isLetter() } || previews.any { quote in it } ||
					lines[line].trimEnd().endsWith("// i18n-ignore")
				if (exempt) null
				else "${file.relativeTo(projectDir)}:${line + 1}: hard-coded UI text, use a string resource: ${lines[line].trim()}"
			}.toList()
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
 * - a placeholder that isn't positional from 1 (%1$s, %1$d), the only kind Compose resources fill in
 * - a translated string whose placeholders differ from the English one
 * - a translated plural without an "other" item, an "other" item without the English one's
 *   placeholders (e.g. the count), or any item with a placeholder the English plural doesn't have
 * - \' in a composeResources string, which Compose resources show as is
 * - an Android res/values-<lang>/strings.xml that misses a translatable string of the English one
 *   (the notification), or has different placeholders
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
		val positional = Regex("""%[1-9]\d*\$[ds]""")
		// values-de, values-pt-rBR
		val languageFolder = Regex("""values-([a-z]{2,3}(?:-r[A-Z]{2})?)""")

		// Translation files come from pull requests, so no external entities or DTDs are read. The
		// DOCTYPE of Info.plist is still allowed, it just isn't fetched.
		fun parse(file: File): Element = DocumentBuilderFactory.newInstance().apply {
			setFeature("http://xml.org/sax/features/external-general-entities", false)
			setFeature("http://xml.org/sax/features/external-parameter-entities", false)
			setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
			isXIncludeAware = false
			isExpandEntityReferences = false
		}.newDocumentBuilder().parse(file).documentElement

		fun Element.children(tag: String): List<Element> {
			val nodes = getElementsByTagName(tag)
			return (0 until nodes.length).map { nodes.item(it) as Element }
		}

		fun strings(file: File): Map<String, String> =
			parse(file).children("string").associate { it.getAttribute("name") to it.textContent }

		// plural key to its items' texts by quantity
		fun plurals(file: File): Map<String, Map<String, String>> =
			parse(file).children("plurals").associate { plural ->
				plural.getAttribute("name") to plural.children("item").associate { it.getAttribute("quantity") to it.textContent }
			}

		fun untranslatableKeys(file: File) =
			parse(file).children("string").filter { it.getAttribute("translatable") == "false" }
				.map { it.getAttribute("name") }.toSet()

		fun placeholders(text: String) = positional.findAll(text).map { it.value }.toSet()

		fun checkStyle(file: File, apostrophes: Boolean) {
			val texts = strings(file).mapValues { listOf(it.value) } + plurals(file).mapValues { it.value.values.toList() }
			texts.forEach { (key, values) ->
				values.forEach { text ->
					if (text.count { it == '%' } != positional.findAll(text).count()) {
						problems += "$file: $key: use positional placeholders numbered from 1 (%1\$s, %1\$d), got \"$text\""
					}
					if (apostrophes && text.contains("\\'")) {
						problems += "$file: $key: write apostrophes as ', Compose resources show \\' as is"
					}
				}
			}
		}

		fun languagesOf(resourceDir: File) = resourceDir.listFiles().orEmpty()
			.mapNotNull { languageFolder.matchEntire(it.name)?.groupValues?.get(1)?.replace("-r", "-") }
			.toSet()

		// Compose resources: every values-<lang> file against the values/ file of the same name
		val baseDir = composeResources.dir("values").asFile
		baseDir.listFiles { file -> file.extension == "xml" }.orEmpty().forEach { checkStyle(it, apostrophes = true) }
		composeResources.asFile.listFiles().orEmpty().filter { languageFolder.matches(it.name) }.forEach { dir ->
			dir.listFiles { file -> file.extension == "xml" }.orEmpty().forEach translations@{ file ->
				val baseFile = File(baseDir, file.name)
				if (!baseFile.exists()) {
					problems += "$file: no values/${file.name} to translate"
					return@translations
				}
				checkStyle(file, apostrophes = true)
				val englishStrings = strings(baseFile)
				val englishPlurals = plurals(baseFile)
				val untranslatable = untranslatableKeys(baseFile)
				strings(file).forEach { (key, text) ->
					val english = englishStrings[key]
					when {
						key in englishPlurals -> problems += "$file: $key must be a plural"
						english == null -> problems += "$file: $key isn't in values/${file.name}"
						key in untranslatable -> problems += "$file: $key is translatable=\"false\""
						placeholders(text) != placeholders(english) ->
							problems += "$file: $key has placeholders ${placeholders(text)}, English has ${placeholders(english)}"
					}
				}
				plurals(file).forEach { (key, items) ->
					val english = englishPlurals[key]
					when {
						key in englishStrings -> problems += "$file: $key must be a string"
						english == null -> problems += "$file: $key isn't in values/${file.name}"
						else -> {
							val other = items["other"]
							if (other == null) {
								problems += "$file: $key needs an item quantity=\"other\""
							} else {
								val englishOther = placeholders(english["other"].orEmpty())
								if (!placeholders(other).containsAll(englishOther)) {
									problems += "$file: $key's \"other\" item misses ${englishOther - placeholders(other)}"
								}
							}
							val allowed = english.values.flatMap(::placeholders).toSet()
							items.forEach { (quantity, text) ->
								val extra = placeholders(text) - allowed
								if (extra.isNotEmpty()) problems += "$file: $key \"$quantity\" has $extra the English plural doesn't"
							}
						}
					}
				}
			}
		}

		// Android's own strings: the notification must be translated in full in every language
		val androidBase = androidResources.file("values/strings.xml").asFile
		checkStyle(androidBase, apostrophes = false)
		val androidEnglish = strings(androidBase) - untranslatableKeys(androidBase)
		languagesOf(androidResources.asFile).forEach { language ->
			val file = androidResources.file("values-${language.replace("-", "-r")}/strings.xml").asFile
			val translated = if (file.exists()) strings(file).also { checkStyle(file, apostrophes = false) } else emptyMap()
			(androidEnglish.keys - translated.keys).forEach { problems += "$file: $it isn't translated" }
			translated.forEach { (key, text) ->
				val english = androidEnglish[key]
				when {
					english == null -> problems += "$file: $key isn't a translatable string of values/strings.xml"
					placeholders(text) != placeholders(english) ->
						problems += "$file: $key has placeholders ${placeholders(text)}, English has ${placeholders(english)}"
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
