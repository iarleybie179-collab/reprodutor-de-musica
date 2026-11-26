package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.formatTrackDuration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Reprodutor de música", appName)
    }

    @Test
    fun `format track duration handles seconds minutes and hours`() {
        assertEquals("00:00", formatTrackDuration(0L))
        assertEquals("03:45", formatTrackDuration(225_000L))
        assertEquals("1:02:05", formatTrackDuration(3_725_000L))
    }
}
