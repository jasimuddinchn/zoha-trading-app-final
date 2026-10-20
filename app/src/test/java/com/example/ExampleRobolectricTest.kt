package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BiometricAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    assertEquals("Zoha Trading", appName)
  }

  @Test
  fun `test biometric preferences management`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Test default is enabled
    assertTrue(BiometricAuthManager.isBiometricEnabled(context))

    // Toggle off
    BiometricAuthManager.setBiometricEnabled(context, false)
    assertFalse(BiometricAuthManager.isBiometricEnabled(context))

    // Toggle back on
    BiometricAuthManager.setBiometricEnabled(context, true)
    assertTrue(BiometricAuthManager.isBiometricEnabled(context))

    // Test profile preference
    BiometricAuthManager.setLastProfile(context, "STAFF_1")
    assertEquals("STAFF_1", BiometricAuthManager.getLastProfile(context))

    BiometricAuthManager.setLastProfile(context, "ADMIN")
    assertEquals("ADMIN", BiometricAuthManager.getLastProfile(context))
  }
}
