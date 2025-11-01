/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inbox.ui.presentation.view.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.ui.PushwooshInboxStyle
import com.pushwoosh.inbox.ui.R
import com.pushwoosh.inbox.ui.presentation.data.UserError
import com.pushwoosh.inbox.ui.presentation.presenter.InboxPresenter
import com.pushwoosh.inbox.ui.presentation.presenter.InboxView
import com.pushwoosh.inbox.ui.presentation.view.activity.AttachmentActivity
import com.pushwoosh.inbox.ui.presentation.view.adapter.SimpleItemTouchHelperCallback
import com.pushwoosh.inbox.ui.presentation.view.adapter.inbox.InboxAdapter
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProvider
import com.pushwoosh.inbox.ui.presentation.view.style.ColorSchemeProviderFactory


open class InboxFragment : BaseFragment(), InboxView {

    private lateinit var inboxAdapter: InboxAdapter
    private lateinit var inboxPresenter: InboxPresenter
    private var callback: SimpleItemTouchHelperCallback? = null
    private lateinit var colorSchemeProvider: ColorSchemeProvider
    private lateinit var inboxRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var inboxSwipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var inboxToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var inboxAppBarLayout: com.google.android.material.appbar.AppBarLayout
    private lateinit var inboxTotalProgressBar: android.widget.ProgressBar
    private lateinit var inboxEmpty: android.widget.LinearLayout
    private lateinit var inboxError: android.widget.LinearLayout
    private lateinit var inboxEmptyTextView: android.widget.TextView
    private lateinit var inboxErrorTextView: android.widget.TextView
    private lateinit var inboxEmptyImageView: android.widget.ImageView
    private lateinit var inboxErrorImageView: android.widget.ImageView


    private var attachmentClickListener: ((String, View) -> Unit) = {url : String, view : View -> onAttachmentClicked(url, view)}

    override fun onAttach(context: Context) {
        inboxPresenter = InboxPresenter(this).apply { addLifecycleListener(this) }
        super.onAttach(context)
        colorSchemeProvider = ColorSchemeProviderFactory.generateColorScheme(context)
        inboxAdapter = InboxAdapter(context, colorSchemeProvider, attachmentClickListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.pw_fragment_inbox, container, false)
        
        // Initialize views immediately when view is created
        inboxRecyclerView = view.findViewById(R.id.inboxRecyclerView)
        inboxSwipeRefreshLayout = view.findViewById(R.id.inboxSwipeRefreshLayout)
        inboxToolbar = view.findViewById(R.id.inboxToolbar)
        inboxAppBarLayout = view.findViewById(R.id.inboxAppBarLayout)
        inboxTotalProgressBar = view.findViewById(R.id.inboxTotalProgressBar)
        inboxEmpty = view.findViewById(R.id.inboxEmpty)
        inboxError = view.findViewById(R.id.inboxError)
        inboxEmptyTextView = view.findViewById(R.id.inboxEmptyTextView)
        inboxErrorTextView = view.findViewById(R.id.inboxErrorTextView)
        inboxEmptyImageView = view.findViewById(R.id.inboxEmptyImageView)
        inboxErrorImageView = view.findViewById(R.id.inboxErrorImageView)
        
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    /**
     * Determines whether InboxFragment should display its own toolbar.
     * 1. Explicit setting (PushwooshInboxStyle.showToolbar) - highest priority
     * 2. Auto-detection (check if host Activity already has ActionBar)
     */
    private fun shouldShowToolbar(): Boolean {
        PushwooshInboxStyle.showToolbar?.let { return it }

        val activity = activity as? androidx.appcompat.app.AppCompatActivity ?: return true
        return activity.supportActionBar == null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the toolbar with back button
        val activity = activity as? androidx.appcompat.app.AppCompatActivity

        if (shouldShowToolbar()) {
            // Show and configure toolbar
            activity?.setSupportActionBar(inboxToolbar)
            activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity?.supportActionBar?.setDisplayShowHomeEnabled(true)

            // Apply custom toolbar title if available
            PushwooshInboxStyle.barTitle?.let { title ->
                activity?.supportActionBar?.title = title
            }

            inboxToolbar.setNavigationOnClickListener {
                activity?.finish()
            }

            // Apply navigation icon tint after it's created
            PushwooshInboxStyle.barTextColor?.let { textColor ->
                inboxToolbar.navigationIcon?.setTint(textColor)
            }
        } else {
            // Hide toolbar when host Activity already has one
            inboxAppBarLayout.visibility = View.GONE
        }

        inboxAdapter.onItemRemoved = { inboxMessage ->
            if (inboxAdapter.itemCount == 0) {
                showEmptyView()
            }

            inboxSwipeRefreshLayout.isEnabled = true
            inboxPresenter.removeItem(inboxMessage)
        }
        inboxAdapter.onItemStartSwipe = { inboxSwipeRefreshLayout.isEnabled = false }
        inboxAdapter.onItemStopSwipe = { inboxSwipeRefreshLayout.isEnabled = true }
        inboxAdapter.onItemClick = { inboxMessage -> if (inboxMessage != null) inboxPresenter.onItemClick(inboxMessage) }

        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        inboxRecyclerView.layoutManager = layoutManager
        inboxRecyclerView.adapter = inboxAdapter

        val backgroundColor = PushwooshInboxStyle.backgroundColor
        if (backgroundColor != null) {
            // Apply background color to all container views for consistency
            view.setBackgroundColor(backgroundColor)
            inboxSwipeRefreshLayout.setBackgroundColor(backgroundColor)
            inboxRecyclerView.setBackgroundColor(backgroundColor)
            inboxEmpty.setBackgroundColor(backgroundColor)
            inboxError.setBackgroundColor(backgroundColor)
            
            // Apply background color to toolbar and app bar (use specific bar color if available)
            val appBarColor = PushwooshInboxStyle.barBackgroundColor ?: backgroundColor
            inboxToolbar.setBackgroundColor(appBarColor)
            inboxAppBarLayout.setBackgroundColor(appBarColor)
            
            // Apply bar text color if available
            PushwooshInboxStyle.barTextColor?.let { textColor ->
                inboxToolbar.setTitleTextColor(textColor)
            }
        }

        val divider = colorSchemeProvider.divider
        if (divider != null) {
            val dividerItemDecoration = androidx.recyclerview.widget.DividerItemDecoration(context, layoutManager.orientation)
            dividerItemDecoration.setDrawable(divider)
            inboxRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        callback = SimpleItemTouchHelperCallback(adapter = inboxAdapter, context = context, colorSchemeProvider = colorSchemeProvider)
        val itemTouchHelper = ItemTouchHelper(callback as ItemTouchHelper.Callback)
        itemTouchHelper.attachToRecyclerView(inboxRecyclerView)

        inboxSwipeRefreshLayout.setOnRefreshListener {
            inboxPresenter.refreshItems()
            callback?.setTouchable(false)
        }

        if (PushwooshInboxStyle.listErrorImageDrawable == null) {
            inboxErrorImageView.setImageResource(PushwooshInboxStyle.listErrorImage)
        } else {
            inboxErrorImageView.setImageDrawable(PushwooshInboxStyle.listErrorImageDrawable)
        }

        if (PushwooshInboxStyle.listEmptyImageDrawable == null) {
            inboxEmptyImageView.setImageResource(PushwooshInboxStyle.listEmptyImage)
        } else {
            inboxEmptyImageView.setImageDrawable(PushwooshInboxStyle.listEmptyImageDrawable)
        }
        
        // Apply proper tinting to images so they're visible on dark backgrounds
        PushwooshInboxStyle.titleColor?.let { color ->
            inboxEmptyImageView.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
            inboxErrorImageView.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        
    }

    override fun showSwipeRefreshProgress() {
        inboxSwipeRefreshLayout.isRefreshing = true
    }

    override fun showTotalProgress() {
        updateContent(showProgress = true)
    }

    override fun hideProgress() {
        updateContent(showProgress = false, swipeRefresh = false)
        callback?.setTouchable(true)
    }

    override fun failedLoadingInboxList(userError: UserError) {
        val localView = view

        if (localView != null) {
            if (inboxAdapter.itemCount != 0) {
                if (TextUtils.isEmpty(userError.message)) {
                    return
                }
                Snackbar.make(localView, userError.message!!, Snackbar.LENGTH_LONG)
                        .show()
            } else {
                updateContent(isError = true)
                updateMessageTextView(inboxErrorTextView, userError.message)
                
                // Apply proper styling to error state text
                PushwooshInboxStyle.titleColor?.let { color ->
                    inboxErrorTextView.setTextColor(color)
                }
            }
        }
    }

    override fun showList(inboxList: Collection<InboxMessage>) {
        inboxAdapter.setCollection(inboxList)
        updateContent(isEmpty = false)
    }

    override fun showEmptyView() {
        updateContent(isEmpty = true)
        inboxSwipeRefreshLayout.isEnabled = true
        updateMessageTextView(inboxEmptyTextView, PushwooshInboxStyle.listEmptyText)
        
        // Apply proper styling to empty state text
        PushwooshInboxStyle.titleColor?.let { color ->
            inboxEmptyTextView.setTextColor(color)
        }
    }

    private fun updateContent(showProgress: Boolean = false, isEmpty: Boolean = false, swipeRefresh: Boolean = false, isError: Boolean = false) {
        val contentVisibility = if (showProgress && !swipeRefresh) View.GONE else View.VISIBLE
        val totalProgressVisibility = if (showProgress && !swipeRefresh) View.VISIBLE else View.GONE

        if (isEmpty) {
            inboxRecyclerView.visibility = View.GONE
            inboxEmpty.visibility = contentVisibility
            inboxError.visibility = View.GONE
        } else if (!isError) {
            inboxRecyclerView.visibility = contentVisibility
            inboxEmpty.visibility = View.GONE
            inboxError.visibility = View.GONE
        } else {
            inboxError.visibility = contentVisibility
            inboxEmpty.visibility = View.GONE
            inboxRecyclerView.visibility = View.GONE
        }

        inboxTotalProgressBar.visibility = totalProgressVisibility
        inboxSwipeRefreshLayout.isRefreshing = swipeRefresh
    }

    private fun updateMessageTextView(messageTextView: TextView, message: CharSequence?) {
        if (TextUtils.isEmpty(message)) {
            messageTextView.visibility = View.GONE
        } else {
            messageTextView.visibility = View.VISIBLE
            messageTextView.text = message
        }
    }

    private fun onAttachmentClicked(url: String, view : View) {
        var intent = Intent(activity, AttachmentActivity::class.java)
        intent.putExtra(AttachmentActivity.attachmentUrlExtra, url)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            var options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(activity as Activity, view, getString(R.string.pw_attachment_transition_id))
            startActivity(intent, options.toBundle())
        } else {
            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent)
        }
    }
}