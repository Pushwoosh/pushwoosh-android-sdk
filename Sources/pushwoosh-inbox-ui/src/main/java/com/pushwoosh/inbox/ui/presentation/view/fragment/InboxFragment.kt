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
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pushwoosh.inbox.PushwooshInbox
import com.pushwoosh.inbox.data.InboxMessage
import com.pushwoosh.inbox.data.InboxTabs
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

    private companion object {
        const val KEY_SELECTED_TAB_ID = "pw_inbox_selected_tab_id"
    }

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
    /**
     * Null when the host app overrides pw_fragment_inbox.xml with a layout that predates the tabs:
     * no panel, and the Inbox stays the plain feed it was before the update.
     */
    private var inboxTabLayout: TabLayout? = null

    private val allMessages = mutableListOf<InboxMessage>()
    private var inboxTabs: InboxTabs? = null
    private var selectedTabId: String? = null
    private var tabSelectionIsBeingRebuilt = false


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
        inboxTabLayout = view.findViewById(R.id.inboxTabLayout)

        // before onViewCreated: the presenter calls showList from super.onViewCreated, already filtered
        selectedTabId = savedInstanceState?.getString(KEY_SELECTED_TAB_ID)
        
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

        // Disable fitsSystemWindows on the root when host is responsible for status-bar inset:
        //  - host Activity has its own actionBar (legacy embed scenario);
        //  - or caller explicitly hides our toolbar (showToolbar=false), e.g. embedding into a
        //    Fragment with custom header, or into an edge-to-edge Activity without supportActionBar.
        // Otherwise (standalone InboxActivity, default embed) keep the inset on the root.
        val isEmbeddedInActivityWithToolbar = activity?.supportActionBar != null
        if (isEmbeddedInActivityWithToolbar || !shouldShowToolbar()) {
            view.fitsSystemWindows = false
        }

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

            applyBarStyle()
        } else {
            // Hide toolbar when host Activity already has one
            inboxAppBarLayout.visibility = View.GONE
        }

        inboxTabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tabSelectionIsBeingRebuilt) return
                selectedTabId = tab.tag as? String
                applyFilter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        inboxAdapter.onItemRemoved = { inboxMessage ->
            if (inboxMessage != null) {
                allMessages.remove(inboxMessage)
            }
            if (messagesForSelectedTab().isEmpty()) {
                renderEmptyState()
            }
            updateTabCounters()

            inboxSwipeRefreshLayout.isEnabled = true
            inboxPresenter.removeItem(inboxMessage)
        }
        inboxAdapter.onItemStartSwipe = { inboxSwipeRefreshLayout.isEnabled = false }
        inboxAdapter.onItemStopSwipe = { inboxSwipeRefreshLayout.isEnabled = true }
        inboxAdapter.onItemClick = { inboxMessage -> if (inboxMessage != null) inboxPresenter.onItemClick(inboxMessage) }

        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        inboxRecyclerView.layoutManager = layoutManager
        inboxRecyclerView.adapter = inboxAdapter
        inboxRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) markDisplayedAsRead()
            }
        })

        PushwooshInboxStyle.backgroundColor?.let { color ->
            view.setBackgroundColor(color)
            inboxSwipeRefreshLayout.setBackgroundColor(color)
            inboxRecyclerView.setBackgroundColor(color)
            inboxEmpty.setBackgroundColor(color)
            inboxError.setBackgroundColor(color)
            inboxTabLayout?.setBackgroundColor(color)
        }

        inboxTabLayout?.setSelectedTabIndicatorColor(colorSchemeProvider.accentColor)
        inboxTabLayout?.setTabTextColors(colorSchemeProvider.titleColor.defaultColor, colorSchemeProvider.accentColor)

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
        inboxSwipeRefreshLayout.setColorSchemeColors(colorSchemeProvider.accentColor)
        inboxTotalProgressBar.indeterminateTintList = ColorStateList.valueOf(colorSchemeProvider.accentColor)

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

    /**
     * Applies the three toolbar keys of [PushwooshInboxStyle]. Each key paints one thing and is
     * independent of the others and of [PushwooshInboxStyle.backgroundColor], which never reaches
     * the bar.
     */
    private fun applyBarStyle() {
        PushwooshInboxStyle.barBackgroundColor?.let { color ->
            inboxToolbar.setBackgroundColor(color)
            inboxAppBarLayout.setBackgroundColor(color)
        }

        PushwooshInboxStyle.barTextColor?.let { color ->
            inboxToolbar.setTitleTextColor(color)
        }

        PushwooshInboxStyle.barAccentColor?.let { color ->
            // a color filter, not setTint: Drawable exposes a getter for it, so the result is readable back
            // mutate() first: AppCompatDrawableManager caches ConstantState, so an unmutated filter leaks onto other back arrows
            val navigationIcon = inboxToolbar.navigationIcon?.mutate()
            navigationIcon?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            inboxToolbar.navigationIcon = navigationIcon
        }
    }

    /**
     * The selected tab outlives a configuration change and nothing else: leaving the Inbox and
     * coming back is an open, and an open starts on "All". A host that keeps the Inbox in a
     * navigation graph hands the fragment the same saved state on re-entry, so the two are told
     * apart here, where only a relaunch reports [android.app.Activity.isChangingConfigurations].
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (activity?.isChangingConfigurations == true) {
            outState.putString(KEY_SELECTED_TAB_ID, selectedTabId)
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

    /**
     * An inbox that holds messages keeps its content and reports the failure in a snackbar; the
     * full-screen error belongs to an inbox with nothing in it, not to an empty tab.
     */
    override fun failedLoadingInboxList(userError: UserError) {
        val localView = view ?: return

        if (allMessages.isNotEmpty()) {
            if (TextUtils.isEmpty(userError.message)) {
                return
            }
            Snackbar.make(localView, userError.message!!, Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        updateContent(isError = true)
        updateMessageTextView(inboxErrorTextView, userError.message)
        PushwooshInboxStyle.titleColor?.let { color ->
            inboxErrorTextView.setTextColor(color)
        }
    }

    override fun showList(inboxList: Collection<InboxMessage>) {
        allMessages.clear()
        allMessages.addAll(inboxList)
        syncTabs()
        applyFilter()
    }

    /**
     * Brings the panel in line with the tabs the backend last sent: no tabs hides it, a changed
     * list redraws it, and a selection that no longer exists falls back to "All".
     */
    private fun syncTabs() {
        val tabLayout = inboxTabLayout
        if (tabLayout == null) {
            selectedTabId = null
            return
        }

        val tabs = PushwooshInbox.getTabs()
        val changed = tabs != inboxTabs
        inboxTabs = tabs

        if (tabs == null) {
            tabLayout.visibility = View.GONE
            tabLayout.removeAllTabs()
            selectedTabId = null
            return
        }

        if (selectedTabId != null && tabs.tabs.none { it.id == selectedTabId }) {
            selectedTabId = null
        }

        if (changed || tabLayout.tabCount != tabs.tabs.size + 1) {
            rebuildTabs(tabLayout, tabs)
        }
        tabLayout.visibility = View.VISIBLE
        updateTabCounters()
    }

    private fun rebuildTabs(tabLayout: TabLayout, tabs: InboxTabs) {
        tabSelectionIsBeingRebuilt = true
        try {
            tabLayout.removeAllTabs()
            tabLayout.addTab(
                tabLayout.newTab().setText(tabLabel(tabs.allTitle, unreadCountFor(null))).setTag(null)
            )
            tabs.tabs.forEach { tab ->
                tabLayout.addTab(
                    tabLayout.newTab().setText(tabLabel(tab.title, unreadCountFor(tab.id))).setTag(tab.id)
                )
            }
            val selected = tabs.tabs.indexOfFirst { it.id == selectedTabId }
            tabLayout.selectTab(tabLayout.getTabAt(if (selected < 0) 0 else selected + 1))
        } finally {
            tabSelectionIsBeingRebuilt = false
        }
    }

    private fun tabLabel(title: String, unread: Int): String = if (unread > 0) "$title  $unread" else title

    private fun unreadCountFor(tabId: String?): Int =
        if (tabId == null) allMessages.count { !it.isRead } else allMessages.count { !it.isRead && it.tabId == tabId }

    private fun updateTabCounters() {
        val tabLayout = inboxTabLayout ?: return
        val tabs = inboxTabs ?: return
        tabLayout.getTabAt(0)?.text = tabLabel(tabs.allTitle, unreadCountFor(null))
        tabs.tabs.forEachIndexed { index, tab ->
            tabLayout.getTabAt(index + 1)?.text = tabLabel(tab.title, unreadCountFor(tab.id))
        }
    }

    /** "All" shows everything, including a message whose tab the backend no longer lists. */
    private fun messagesForSelectedTab(): List<InboxMessage> {
        val tabId = selectedTabId ?: return allMessages.toList()
        return allMessages.filter { it.tabId == tabId }
    }

    private fun applyFilter() {
        val visible = messagesForSelectedTab()
        inboxAdapter.setCollection(visible)
        if (visible.isEmpty()) {
            renderEmptyState()
        } else {
            updateContent(isEmpty = false)
            inboxRecyclerView.post { markDisplayedAsRead() }
        }
    }

    private fun renderEmptyState() {
        updateContent(isEmpty = true)
        inboxSwipeRefreshLayout.isEnabled = true
        updateMessageTextView(inboxEmptyTextView, PushwooshInboxStyle.listEmptyText)
        PushwooshInboxStyle.titleColor?.let { color -> inboxEmptyTextView.setTextColor(color) }
    }

    override fun onResume() {
        super.onResume()
        inboxRecyclerView.post { markDisplayedAsRead() }
    }

    /** Reads the rows on screen while the inbox is showing them; already-read rows are skipped. */
    internal fun markDisplayedAsRead() {
        if (!PushwooshInboxStyle.automaticReadOnDisplay || !isResumed) return
        val layoutManager = inboxRecyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last < first) return
        val codes = (first..last).mapNotNull { inboxAdapter.getItem(it) }.filter { !it.isRead }.map { it.code }
        if (codes.isNotEmpty()) PushwooshInbox.readMessages(codes)
    }

    override fun showEmptyView() {
        allMessages.clear()
        syncTabs()
        inboxAdapter.setCollection(emptyList())
        renderEmptyState()
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
        val activity = activity ?: return
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
