package com.personaltracker.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun testMigration2To3ExecutesExpectedSqlStatements() {
        val executedSql = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_2_3.migrate(dbProxy)

        assertEquals(7, executedSql.size)
        assertTrue(executedSql.any { it.contains("ADD COLUMN cadenceType") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN weeklyTargetFrequency") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN reminderTime") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN isReminderEnabled") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN goalTargetCount") })
        assertTrue(executedSql.any { it.contains("ADD COLUMN category") })
        assertTrue(executedSql.any { it.contains("UPDATE habits SET cadenceType = 'SPECIFIC_DAYS'") })
    }
}
