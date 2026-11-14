package com.personaltracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personaltracker.data.local.db.converters.DateConverters
import com.personaltracker.data.local.db.converters.EncryptedPayloadConverter
import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.CompanionProfileDao
import com.personaltracker.data.local.db.daos.DayOverrideDao
import com.personaltracker.data.local.db.daos.DayTypeDao
import com.personaltracker.data.local.db.daos.HabitDao
import com.personaltracker.data.local.db.daos.InterventionRuleDao
import com.personaltracker.data.local.db.daos.RoutineDao
import com.personaltracker.data.local.db.daos.ShopItemDao
import com.personaltracker.data.local.db.daos.StudySessionDao
import com.personaltracker.data.local.db.daos.TaskDao
import com.personaltracker.data.local.db.daos.UnfiledInboxDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.data.local.db.entities.CompanionProfileEntity
import com.personaltracker.data.local.db.entities.DayOverrideEntity
import com.personaltracker.data.local.db.entities.DayTypeEntity
import com.personaltracker.data.local.db.entities.HabitEntity
import com.personaltracker.data.local.db.entities.HabitLogEntity
import com.personaltracker.data.local.db.entities.InterventionRuleEntity
import com.personaltracker.data.local.db.entities.RoutineEntity
import com.personaltracker.data.local.db.entities.RoutineStepEntity
import com.personaltracker.data.local.db.entities.ShopItemEntity
import com.personaltracker.data.local.db.entities.StudySessionEntity
import com.personaltracker.data.local.db.entities.TaskEntity
import com.personaltracker.data.local.db.entities.UnfiledCaptureInboxEntity

/**
 * Main Room Database for Personal-Tracker v2.
 * Encrypted at rest over SQLCipher with Android Keystore hardware-backed key protection.
 *
 * Implements ARCH-2, NFR-4, and Story 1.2 / Story 3.4 specifications.
 */
@Database(
    entities = [
        DayTypeEntity::class,
        DayOverrideEntity::class,
        TaskEntity::class,
        RoutineEntity::class,
        RoutineStepEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        StudySessionEntity::class,
        CoinLedgerEntity::class,
        CompanionProfileEntity::class,
        ShopItemEntity::class,
        UnfiledCaptureInboxEntity::class,
        InterventionRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(EncryptedPayloadConverter::class, DateConverters::class)
abstract class AppDatabase : RoomDatabase() {

    internal abstract fun dayTypeDao(): DayTypeDao
    internal abstract fun dayOverrideDao(): DayOverrideDao
    internal abstract fun taskDao(): TaskDao
    internal abstract fun routineDao(): RoutineDao
    internal abstract fun habitDao(): HabitDao
    internal abstract fun studySessionDao(): StudySessionDao
    internal abstract fun coinLedgerDao(): CoinLedgerDao
    internal abstract fun companionProfileDao(): CompanionProfileDao
    internal abstract fun shopItemDao(): ShopItemDao
    internal abstract fun unfiledInboxDao(): UnfiledInboxDao
    internal abstract fun interventionRuleDao(): InterventionRuleDao

    companion object {
        const val DATABASE_NAME = "personal_tracker_encrypted.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN targetDurationMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN actualElapsedMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN intervalType TEXT NOT NULL DEFAULT 'DEEP_WORK'")
            }
        }
    }
}
