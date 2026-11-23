package com.personaltracker.platform.notification

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TieredNotificationManagerTest {

    @Test
    fun `channel constants match specifications`() {
        assertEquals("channel_routine", TieredNotificationManager.CHANNEL_ID_ROUTINE)
        assertEquals("channel_important", TieredNotificationManager.CHANNEL_ID_IMPORTANT)
        assertEquals("channel_urgent", TieredNotificationManager.CHANNEL_ID_URGENT)
        assertEquals("task_reminders_channel", TieredNotificationManager.LEGACY_CHANNEL_ID)
    }

    @Test
    fun `urgent vibration pattern matches companion double-pulse specifications`() {
        val expectedPattern = longArrayOf(0, 250, 150, 250)
        assertArrayEquals(expectedPattern, TieredNotificationManager.URGENT_VIBRATION_PATTERN)
    }

    @Test
    fun `getChannelIdForIntensity maps URGENT correctly`() {
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_URGENT,
            TieredNotificationManager.getChannelIdForIntensity("URGENT")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_URGENT,
            TieredNotificationManager.getChannelIdForIntensity("urgent")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_URGENT,
            TieredNotificationManager.getChannelIdForIntensity("  URGENT  ")
        )
    }

    @Test
    fun `getChannelIdForIntensity maps IMPORTANT correctly`() {
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_IMPORTANT,
            TieredNotificationManager.getChannelIdForIntensity("IMPORTANT")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_IMPORTANT,
            TieredNotificationManager.getChannelIdForIntensity("important")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_IMPORTANT,
            TieredNotificationManager.getChannelIdForIntensity("  important  ")
        )
    }

    @Test
    fun `getChannelIdForIntensity falls back to ROUTINE for routine, null, or unrecognized strings`() {
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_ROUTINE,
            TieredNotificationManager.getChannelIdForIntensity("ROUTINE")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_ROUTINE,
            TieredNotificationManager.getChannelIdForIntensity("routine")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_ROUTINE,
            TieredNotificationManager.getChannelIdForIntensity(null)
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_ROUTINE,
            TieredNotificationManager.getChannelIdForIntensity("")
        )
        assertEquals(
            TieredNotificationManager.CHANNEL_ID_ROUTINE,
            TieredNotificationManager.getChannelIdForIntensity("UNKNOWN_INTENSITY")
        )
    }
}
