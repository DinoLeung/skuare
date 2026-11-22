package xyz.d1n0.phone

import dev.icerock.moko.permissions.Permission
import kotlin.time.Duration

/**
 * A position of the phone, only fields both android.location and CoreLocation give.
 *
 * @property accuracyMeters Radius of 68% confidence around the position, null if unknown.
 */
data class GeoPoint(
	val latitude: Double,
	val longitude: Double,
	val accuracyMeters: Double? = null,
)

/**
 * What asking for the phone's location gave, a failure is a result rather than an exception, so a
 * background session can log it and carry on.
 */
sealed interface LocationResult {
	data class Fix(val point: GeoPoint) : LocationResult

	/** Location isn't allowed, or only while the app is open and it's in the background. */
	data object PermissionDenied : LocationResult

	/** Location services are off. */
	data object Disabled : LocationResult

	/** No fix in time, or the platform gave up on one, and no recent last known location. */
	data object TimedOut : LocationResult

	data class Failed(val error: Throwable) : LocationResult
}

/**
 * Reads the phone's location, for recording where the watch's lower-right button was pressed.
 * Implemented per platform, see research/button-actions-plan.md.
 */
interface LocationProvider {
	/** False where location can't be read in a background session, the feature is hidden then. */
	val isSupported: Boolean

	/**
	 * Permissions to request, in this order, before the feature is turned on. Android asks for
	 * background location on its own after the foreground one, it ignores a request for both.
	 */
	val requiredPermissions: List<Permission>

	/** Whether a background session may read the location, checked without any UI. */
	fun isPermissionGranted(): Boolean

	/**
	 * A fresh fix. Suspends until one arrives, the caller decides how long to wait and cancels it.
	 * [LocationResult.TimedOut] if the platform gave up on its own. Never throws for a missing
	 * permission or disabled location services, see [LocationResult].
	 */
	suspend fun currentLocation(): LocationResult

	/**
	 * The newest location the phone already has, if it's younger than [maxAge], the fallback when
	 * no fresh fix came in time. Null without one, or without the permission.
	 */
	suspend fun lastKnownLocation(maxAge: Duration): GeoPoint?
}

/** For platforms that can't read the location in the background yet, the feature stays hidden. */
class UnsupportedLocationProvider : LocationProvider {
	override val isSupported = false
	override val requiredPermissions = emptyList<Permission>()
	override fun isPermissionGranted() = false
	override suspend fun currentLocation() = LocationResult.PermissionDenied
	override suspend fun lastKnownLocation(maxAge: Duration): GeoPoint? = null
}
