package xyz.d1n0.ui.screen.appsettings

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController

/**
 * The part of moko's [PermissionsController] the App settings screen uses. moko's interface has
 * Android-only members, so it can't be faked in common tests, this can.
 */
interface PermissionRequester {
	suspend fun isGranted(permission: Permission): Boolean

	/**
	 * Shows the system's prompt for [permission].
	 *
	 * @throws Exception If it isn't granted, e.g. moko's DeniedException.
	 */
	suspend fun request(permission: Permission)

	/** Opens the system settings page of the app, where its permissions are changed. */
	fun openAppSettings()
}

fun PermissionsController.asRequester(): PermissionRequester = object : PermissionRequester {
	override suspend fun isGranted(permission: Permission) =
		getPermissionState(permission) == PermissionState.Granted

	override suspend fun request(permission: Permission) = providePermission(permission)

	override fun openAppSettings() = this@asRequester.openAppSettings()
}
