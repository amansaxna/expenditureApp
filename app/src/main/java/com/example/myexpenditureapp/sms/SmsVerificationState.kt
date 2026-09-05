package com.example.myexpenditureapp.sms

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsVerificationState {
    private val _smsParsedEvent = MutableSharedFlow<String>()
    val smsParsedEvent = _smsParsedEvent.asSharedFlow()

    suspend fun notifySmsParsed(message: String) {
        _smsParsedEvent.emit(message)
    }
}
