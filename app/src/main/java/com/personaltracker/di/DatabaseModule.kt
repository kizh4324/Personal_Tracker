package com.personaltracker.di

import android.content.Context
import androidx.room.Room
import com.personaltracker.data.local.db.AppDatabase
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
import com.personaltracker.data.local.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module providing the encrypted Room database and its DAOs.
 *
 * Implements ARCH-2, ARCH-9, and Story 1.2 specifications.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider
    ): AppDatabase {
        val passphrase = keyProvider.getDatabasePassphrase()

        val hook = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection) {
                connection.executeRaw("PRAGMA cipher_default_kdf_iter = 256000;", null, null)
                connection.executeRaw("PRAGMA cipher_default_kdf_algorithm = PBKDF2_HMAC_SHA512;", null, null)
            }

            override fun postKey(connection: SQLiteConnection) {
                connection.executeRaw("PRAGMA cipher_page_size = 4096;", null, null)
                connection.executeRaw("PRAGMA kdf_iter = 256000;", null, null)
            }
        }

        val openHelperFactory = SupportOpenHelperFactory(passphrase, hook, false)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(openHelperFactory)
            .build()
    }

    @Provides
    fun provideDayTypeDao(db: AppDatabase): DayTypeDao = db.dayTypeDao()

    @Provides
    fun provideDayOverrideDao(db: AppDatabase): DayOverrideDao = db.dayOverrideDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideRoutineDao(db: AppDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()

    @Provides
    fun provideCoinLedgerDao(db: AppDatabase): CoinLedgerDao = db.coinLedgerDao()

    @Provides
    fun provideCompanionProfileDao(db: AppDatabase): CompanionProfileDao = db.companionProfileDao()

    @Provides
    fun provideShopItemDao(db: AppDatabase): ShopItemDao = db.shopItemDao()

    @Provides
    fun provideUnfiledInboxDao(db: AppDatabase): UnfiledInboxDao = db.unfiledInboxDao()

    @Provides
    fun provideInterventionRuleDao(db: AppDatabase): InterventionRuleDao = db.interventionRuleDao()
}
