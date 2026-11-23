package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.catalog.Catalog
import com.pushwoosh.pushmart.databinding.FragmentPreferencesBinding
import com.pushwoosh.pushmart.databinding.ViewCategoryTileBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.shop.PreferencesStore
import com.pushwoosh.pushmart.ui.common.ProductImages
import com.pushwoosh.pushmart.ui.common.ResultBus
import com.pushwoosh.pushmart.ui.common.categoryTitle
import kotlin.math.roundToInt

/**
 * Profile > Shopping preferences, as iOS `TagsView`: favourite categories, size, news topics and
 * the email opt-in, restored from the device's tags on open.
 */
class PreferencesFragment : Fragment(R.layout.fragment_preferences) {
    private var binding: FragmentPreferencesBinding? = null
    private var topicsDraft: MutableSet<String>? = null
    private val tiles = mutableMapOf<String, ViewCategoryTileBinding>()
    private val sizeChips = mutableMapOf<String, TextView>()
    private val topicChips = mutableMapOf<String, TextView>()
    private val onEmailNews =
        CompoundButton.OnCheckedChangeListener { _, checked ->
            val failed = getString(R.string.prefs_email_news_failed)
            services.preferences.setEmailNews(checked, services.account.userEmail) {
                render()
                ResultBus.fail(failed)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPreferencesBinding.bind(view)
        this.binding = binding
        val preferences = services.preferences
        topicsDraft =
            savedInstanceState?.getStringArrayList(KEY_TOPICS_DRAFT)?.toMutableSet()
                ?: topicsDraft
                ?: preferences.newsTopics.toMutableSet()
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }

        buildCategoryGrid(binding.categoryGrid)
        buildSizeRow(binding.sizeRow)
        buildTopicChips(binding.topicChips)

        binding.newsRow.pageRowIcon.setImageResource(R.drawable.ic_newspaper)
        binding.newsRow.pageRowTitle.setText(R.string.prefs_news_topics)
        binding.newsRow.pageRowDetail.setText(R.string.prefs_news_topics_detail)
        binding.saveTopics.setOnClickListener { saveTopics() }

        val emailRow = binding.emailRow
        emailRow.pageRowIcon.setImageResource(R.drawable.ic_mail)
        emailRow.pageRowTitle.setText(R.string.prefs_email_news)
        emailRow.pageRowSwitch.isVisible = true
        emailRow.pageRowSwitch.contentDescription = getString(R.string.prefs_email_news)

        render()
        preferences.restore { render() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        topicsDraft?.let { outState.putStringArrayList(KEY_TOPICS_DRAFT, ArrayList(it)) }
    }

    override fun onDestroyView() {
        tiles.clear()
        sizeChips.clear()
        topicChips.clear()
        binding = null
        super.onDestroyView()
    }

    private fun buildCategoryGrid(grid: LinearLayout) {
        tiles.clear()
        val gap = dp(GRID_GAP_DP)
        Catalog.preferenceCategories.chunked(2).forEachIndexed { rowIndex, row ->
            val line = LinearLayout(grid.context)
            line.orientation = LinearLayout.HORIZONTAL
            val lineParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (rowIndex > 0) lineParams.topMargin = gap
            grid.addView(line, lineParams)
            row.forEachIndexed { index, category ->
                val tile = ViewCategoryTileBinding.inflate(layoutInflater, line, false)
                if (index > 0)
                    (tile.root.layoutParams as LinearLayout.LayoutParams).marginStart = gap
                tile.root.clipToOutline = true
                val title = requireContext().categoryTitle(category)
                tile.tileTitle.text = title
                tile.root.contentDescription = title
                Catalog.all
                    .firstOrNull { it.category == category }
                    ?.let { ProductImages.bind(tile.tileImage, it) }
                tile.root.setOnClickListener {
                    services.preferences.toggle(category)
                    render()
                }
                line.addView(tile.root)
                tiles[category] = tile
            }
            if (row.size == 1) {
                val filler = LinearLayout.LayoutParams(0, 1, 1f)
                filler.marginStart = gap
                line.addView(Space(line.context), filler)
            }
        }
    }

    private fun buildSizeRow(row: LinearLayout) {
        sizeChips.clear()
        PreferencesStore.SIZES.forEachIndexed { index, value ->
            val chip = TextView(row.context, null, 0, R.style.Widget_PushMart_Chip_Solid)
            chip.text = value
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, SIZE_TEXT_SP)
            chip.setPadding(0, 0, 0, 0)
            val params = LinearLayout.LayoutParams(0, dp(SIZE_HEIGHT_DP), 1f)
            if (index > 0) params.marginStart = dp(SIZE_GAP_DP)
            chip.setOnClickListener {
                val failed = getString(R.string.prefs_size_failed)
                services.preferences.selectSize(value) {
                    render()
                    ResultBus.fail(failed)
                }
                render()
            }
            row.addView(chip, params)
            sizeChips[value] = chip
        }
    }

    private fun buildTopicChips(group: ViewGroup) {
        topicChips.clear()
        for (id in PreferencesStore.NEWS_TOPICS) {
            val chip = TextView(group.context, null, 0, R.style.Widget_PushMart_Chip_Brand)
            chip.setText(topicTitle(id))
            chip.setOnClickListener {
                val draft = topicsDraft ?: return@setOnClickListener
                if (!draft.remove(id)) draft.add(id)
                render()
            }
            group.addView(
                chip,
                ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            topicChips[id] = chip
        }
    }

    private fun saveTopics() {
        val draft = topicsDraft ?: return
        val saved = getString(R.string.prefs_topics_saved)
        val failed = getString(R.string.prefs_topics_failed)
        services.preferences.saveNewsTopics(PreferencesStore.NEWS_TOPICS.filter(draft::contains)) {
            ok ->
            if (ok) ResultBus.success(saved) else ResultBus.fail(failed)
        }
    }

    private fun render() {
        val binding = binding ?: return
        val preferences = services.preferences
        for ((category, tile) in tiles) {
            val on = preferences.isSelected(category)
            tile.root.isSelected = on
            tile.tileBadge.setImageResource(if (on) R.drawable.ic_check else R.drawable.ic_add)
        }
        val size = preferences.size
        for ((value, chip) in sizeChips) chip.isSelected = value == size
        val draft = topicsDraft.orEmpty()
        for ((id, chip) in topicChips) {
            val on = id in draft
            chip.isSelected = on
            val check =
                if (on) AppCompatResources.getDrawable(chip.context, R.drawable.ic_check)?.mutate()
                else null
            check?.setBounds(0, 0, dp(CHECK_ICON_DP), dp(CHECK_ICON_DP))
            chip.setCompoundDrawablesRelative(check, null, null, null)
            TextViewCompat.setCompoundDrawableTintList(chip, chip.textColors)
        }

        val email = services.account.userEmail
        val emailRow = binding.emailRow
        if (email.isEmpty()) {
            emailRow.pageRowDetail.setText(R.string.prefs_email_news_no_email)
        } else {
            emailRow.pageRowDetail.text = email
        }
        val emailSwitch = emailRow.pageRowSwitch
        emailSwitch.setOnCheckedChangeListener(null)
        emailSwitch.isChecked = preferences.emailNews
        emailSwitch.isEnabled = email.isNotEmpty()
        emailSwitch.setOnCheckedChangeListener(onEmailNews)
    }

    private fun topicTitle(id: String): Int =
        when (id) {
            "sales" -> R.string.prefs_topic_sales
            "drops" -> R.string.prefs_topic_drops
            else -> R.string.prefs_topic_new_in
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val KEY_TOPICS_DRAFT = "topicsDraft"
        private const val GRID_GAP_DP = 12
        private const val SIZE_HEIGHT_DP = 48
        private const val SIZE_GAP_DP = 8
        private const val SIZE_TEXT_SP = 16f
        private const val CHECK_ICON_DP = 14
    }
}
