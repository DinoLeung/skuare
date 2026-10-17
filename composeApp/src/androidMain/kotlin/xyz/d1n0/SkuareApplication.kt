package xyz.d1n0

import android.app.Application
import org.koin.android.ext.koin.androidContext

/**
 * Starts Koin for the whole process, so background services can use it without the UI.
 */
class SkuareApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		initKoin {
			androidContext(this@SkuareApplication)
		}
	}
}
