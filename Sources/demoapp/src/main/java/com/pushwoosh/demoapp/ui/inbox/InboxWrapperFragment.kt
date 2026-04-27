package com.pushwoosh.demoapp.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pushwoosh.demoapp.R
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.presentation.view.fragment.InboxFragment

class InboxWrapperFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushwooshInboxStyle.showToolbar = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_inbox_wrapper, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (childFragmentManager.findFragmentById(R.id.inbox_container) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.inbox_container, InboxFragment())
                .commitNow()
        }
    }

    override fun onResume() {
        super.onResume()
        PushwooshInboxStyle.showToolbar = false
    }

    override fun onPause() {
        super.onPause()
        PushwooshInboxStyle.showToolbar = null
    }
}
