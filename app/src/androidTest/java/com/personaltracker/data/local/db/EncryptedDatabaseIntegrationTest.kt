package com.personaltracker.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.personaltracker.data.local.db.entities.DayTypeEntity
import com.personaltracker.data.local.db.entities.TaskEntity
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.security.SecureRandom
import java.util.UUID

/**
 * Instrumented integration tests verifying SQLCipher at-rest encryption,
 * raw header ciphertext verification, wrong-passphrase rejection,
 * and unencrypted framework SQLite rejection (ARCH-2, NFR-4, Story 1.2 AC).
 */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private val testPassphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    private val wrongPassphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }

    private val hook = object : SQLiteDatabaseHook {
        override fun preKey(connection: SQLiteConnection) {
            connection.executeRaw("PRAGMA cipher_page_size = 4096;", null, null)
            connection.executeRaw("PRAGMA cipher_default_kdf_iter = 256000;", null, null)
            connection.executeRaw("PRAGMA cipher_default_kdf_algorithm = PBKDF2_HMAC_SHA512;", null, null)
        }

        override fun postKey(connection: SQLiteConnection) {
            connection.executeRaw("PRAGMA cipher_page_size = 4096;", null, null)
            connection.executeRaw("PRAGMA kdf_iter = 256000;", null, null)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath("test_encrypted_integration.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @After
    fun tearDown() {
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    private fun buildEncryptedDb(passphrase: ByteArray): AppDatabase {
        val factory = SupportOpenHelperFactory(passphrase, hook, false)
        return Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .openHelperFactory(factory)
            .build()
    }

    @Test
    fun testEncryptedDatabaseCrudAndRoundTrip() = runBlocking {
        // 1. Initialize and insert data
        val db = buildEncryptedDb(testPassphrase)
        val taskDao = db.taskDao()

        val taskId = UUID.randomUUID().toString()
        val testTask = TaskEntity(
            id = taskId,
            title = "Encrypted Task Test",
            description = "Confidential test payload",
            scheduledDate = "2026-08-17",
            scheduledTime = "10:00",
            estimatedDurationMinutes = 45,
            deliveryIntensity = "URGENT",
            state = "PENDING",
            isCarryForward = false,
            carryForwardCount = 0,
            completionTimestamp = null,
            createdTimestamp = System.currentTimeMillis()
        )

        taskDao.insertTask(testTask)
        val retrieved = taskDao.getTaskById(taskId)
        assertNotNull("Retrieved task must not be null", retrieved)
        assertEquals("Title must match inserted entity", "Encrypted Task Test", retrieved?.title)

        db.close()

        // 2. Re-open with valid passphrase and verify persistence
        val reOpenedDb = buildEncryptedDb(testPassphrase)
        val reRetrieved = reOpenedDb.taskDao().getTaskById(taskId)
        assertNotNull("Task must persist across database close/re-open", reRetrieved)
        assertEquals("Encrypted Task Test", reRetrieved?.title)
        reOpenedDb.close()
    }

    @Test
    fun testRawDatabaseHeaderIsNotPlaintextSqlite() = runBlocking {
        val db = buildEncryptedDb(testPassphrase)
        db.dayTypeDao().insertDayType(
            DayTypeEntity(
                id = "test_day",
                name = "Test Day",
                colorHex = "#2563EB",
                targetStudyMinutes = 60,
                isDefault = false,
                defaultDaysOfWeek = "1"
            )
        )
        db.close()

        assertTrue("Database file must exist on disk", dbFile.exists())
        assertTrue("Database file size must be > 0", dbFile.length() > 0)

        // Read first 16 bytes of the raw database file
        val headerBytes = ByteArray(16)
        FileInputStream(dbFile).use { fis ->
            fis.read(headerBytes)
        }

        // Standard SQLite magic header is "SQLite format 3\0"
        val standardSqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        assertFalse(
            "Raw database header must NOT contain standard plaintext SQLite magic header (must be encrypted ciphertext)",
            headerBytes.contentEquals(standardSqliteHeader)
        )
    }

    @Test
    fun testUnencryptedFrameworkSqliteRejection() = runBlocking {
        val db = buildEncryptedDb(testPassphrase)
        db.dayTypeDao().insertDayType(
            DayTypeEntity(
                id = "test_day",
                name = "Test Day",
                colorHex = "#2563EB",
                targetStudyMinutes = 60,
                isDefault = false,
                defaultDaysOfWeek = "1"
            )
        )
        db.close()

        try {
            val unencryptedDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val cursor = unencryptedDb.rawQuery("SELECT * FROM day_types", null)
            cursor.close()
            unencryptedDb.close()
            fail("Unencrypted framework SQLite must reject encrypted SQLCipher database")
        } catch (e: android.database.sqlite.SQLiteException) {
            // Expected: SQLiteException or SQLiteDatabaseCorruptException
            assertTrue("Expected SQLiteException on unencrypted read attempt", true)
        }
    }

    @Test
    fun testWrongPassphraseRejection() = runBlocking {
        val db = buildEncryptedDb(testPassphrase)
        db.dayTypeDao().insertDayType(
            DayTypeEntity(
                id = "test_day",
                name = "Test Day",
                colorHex = "#2563EB",
                targetStudyMinutes = 60,
                isDefault = false,
                defaultDaysOfWeek = "1"
            )
        )
        db.close()

        val wrongDb = buildEncryptedDb(wrongPassphrase)
        try {
            wrongDb.dayTypeDao().getDayTypeById("test_day")
            fail("Query with wrong passphrase must fail")
        } catch (e: Exception) {
            // Expected: net.zetetic.database.sqlcipher.SQLiteException / file is not a database
            assertTrue("Expected exception when opening with incorrect passphrase", true)
        } finally {
            try {
                wrongDb.close()
            } catch (ignored: Exception) {
            }
        }
    }
}
