package com.ani.map

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ani.map", appContext.packageName)
    }

    @Test
    fun appHasInternetPermission() {
        // Verifies that the app has the INTERNET permission
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hasPermission = appContext.checkCallingOrSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED
        assertTrue("App should have INTERNET permission", hasPermission)
    }

}