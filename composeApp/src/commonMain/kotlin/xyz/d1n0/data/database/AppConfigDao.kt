package xyz.d1n0.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppConfigDao {
	@Query("SELECT * FROM app_config WHERE id = 0")
	suspend fun get(): AppConfig?

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertIfMissing(config: AppConfig)
}
