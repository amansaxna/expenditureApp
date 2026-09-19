package com.example.myexpenditureapp.sms

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsVerificationStateTest {

    @Test
    fun testNotifySmsParsedEmitsEvent() = runTest {
        val testMessage = "Parsed transaction for Rs. 500 at Swiggy"
        var received: String? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            received = SmsVerificationState.smsParsedEvent.first()
        }

        SmsVerificationState.notifySmsParsed(testMessage)

        assertEquals(testMessage, received)
    }
}
