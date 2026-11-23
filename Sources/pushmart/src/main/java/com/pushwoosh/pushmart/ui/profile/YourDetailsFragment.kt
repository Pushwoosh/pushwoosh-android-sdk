package com.pushwoosh.pushmart.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentYourDetailsBinding
import com.pushwoosh.pushmart.services
import com.pushwoosh.pushmart.ui.common.ResultBus
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Profile > Your details, as iOS `UserView`: the profile photo, the email (`setEmail`) and the SMS
 * and WhatsApp numbers for order updates.
 */
class YourDetailsFragment : Fragment(R.layout.fragment_your_details) {
    private var binding: FragmentYourDetailsBinding? = null
    private var hasPhoto = false
    private var photoOptions: AlertDialog? = null
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) usePhoto(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentYourDetailsBinding.bind(view)
        this.binding = binding
        binding.backButton.back.setOnClickListener { findNavController().navigateUp() }
        binding.avatarImage.clipToOutline = true
        binding.avatarButton.setOnClickListener {
            if (hasPhoto) showPhotoOptions() else choosePhoto()
        }
        if (savedInstanceState == null) {
            val account = services.account
            binding.emailInput.setText(account.userEmail)
            binding.smsInput.setText(account.smsNumber)
            binding.whatsappInput.setText(account.whatsappNumber)
        }
        binding.save.setOnClickListener {
            services.accountManager.saveDetails(
                binding.emailInput.text.toString(),
                binding.smsInput.text.toString(),
                binding.whatsappInput.text.toString()) { outcome ->
                    ResultBus.show(outcome)
                }
            renderHeader()
        }
        renderHeader()
    }

    override fun onDestroyView() {
        photoOptions?.dismiss()
        photoOptions = null
        binding = null
        super.onDestroyView()
    }

    private fun renderHeader() {
        val binding = binding ?: return
        val email = services.account.userEmail
        val photo = services.avatar.load()
        hasPhoto = photo != null
        val image = binding.avatarImage
        val initial = email.firstOrNull()?.uppercase()
        binding.avatarInitial.isVisible = photo == null && initial != null
        binding.avatarInitial.text = initial
        when {
            photo != null -> {
                image.scaleType = ImageView.ScaleType.CENTER_CROP
                image.setPadding(0, 0, 0, 0)
                ImageViewCompat.setImageTintList(image, null)
                image.setImageBitmap(photo)
            }
            initial != null -> image.setImageDrawable(null)
            else -> {
                val padding = (PERSON_PADDING_DP * resources.displayMetrics.density).roundToInt()
                image.scaleType = ImageView.ScaleType.FIT_CENTER
                image.setPadding(padding, padding, padding, padding)
                ImageViewCompat.setImageTintList(
                    image,
                    ContextCompat.getColorStateList(requireContext(), R.color.pm_text_secondary))
                image.setImageResource(R.drawable.ic_person)
            }
        }
        if (email.isEmpty()) {
            binding.subtitle.setText(R.string.your_details_subtitle)
        } else {
            binding.subtitle.text = email
        }
    }

    private fun choosePhoto() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showPhotoOptions() {
        photoOptions?.dismiss()
        val items =
            arrayOf(
                getString(R.string.your_details_photo_choose),
                getString(R.string.your_details_photo_remove))
        photoOptions =
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_PushMart_AlertDialog)
                .setTitle(R.string.your_details_photo_title)
                .setItems(items) { _, which ->
                    if (which == 0) {
                        choosePhoto()
                    } else {
                        services.avatar.remove()
                        renderHeader()
                    }
                }
                .show()
    }

    private fun usePhoto(uri: Uri) {
        val view = view ?: return
        val context = requireContext().applicationContext
        val avatar = services.avatar
        thread(name = "pushmart-avatar") {
            if (avatar.save(context.contentResolver, uri)) {
                ResultBus.success(context.getString(R.string.your_details_photo_updated))
            } else {
                ResultBus.fail(
                    context.getString(R.string.your_details_photo_failed),
                    context.getString(R.string.your_details_photo_failed_message))
            }
            view.post { renderHeader() }
        }
    }

    companion object {
        private const val PERSON_PADDING_DP = 26
    }
}
