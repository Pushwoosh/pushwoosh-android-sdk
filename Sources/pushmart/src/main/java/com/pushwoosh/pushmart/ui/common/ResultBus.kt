package com.pushwoosh.pushmart.ui.common

import androidx.lifecycle.MutableLiveData
import com.pushwoosh.pushmart.account.AccountOutcome

object ResultBus {
    data class Event(val success: Boolean, val title: String, val message: String)

    val events = MutableLiveData<Event?>()

    fun success(title: String, message: String = "") {
        events.postValue(Event(true, title, message))
    }

    fun fail(title: String, message: String = "") {
        events.postValue(Event(false, title, message))
    }

    fun show(outcome: AccountOutcome) {
        when (outcome) {
            is AccountOutcome.Success -> success(outcome.title, outcome.message)
            is AccountOutcome.Failure -> fail(outcome.title, outcome.message)
        }
    }
}
