package xyz.d1n0.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
	/** Null until a setting was first changed, every setting has its default then. */
	@Query("SELECT * FROM app_settings WHERE id = 0")
	suspend fun get(): AppSettings?

	/** See [get]. */
	@Query("SELECT * FROM app_settings WHERE id = 0")
	fun observe(): Flow<AppSettings?>

	/** Creates the row with every default, before an update can change one setting in it. */
	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertIfMissing(settings: AppSettings)

	// The setters below do nothing until the row exists, see insertIfMissing.

	@Query("UPDATE app_settings SET locationRecordingEnabled = :enabled WHERE id = 0")
	suspend fun setLocationRecordingEnabled(enabled: Boolean)

	@Query("UPDATE app_settings SET findPhoneEnabled = :enabled WHERE id = 0")
	suspend fun setFindPhoneEnabled(enabled: Boolean)

	@Query("UPDATE app_settings SET findPhoneRingUntilStopped = :untilStopped WHERE id = 0")
	suspend fun setFindPhoneRingUntilStopped(untilStopped: Boolean)

	@Query("UPDATE app_settings SET findPhoneRingSeconds = :seconds WHERE id = 0")
	suspend fun setFindPhoneRingSeconds(seconds: Int)

	@Query("UPDATE app_settings SET findPhoneSoundName = :name WHERE id = 0")
	suspend fun setFindPhoneSoundName(name: String?)
}
