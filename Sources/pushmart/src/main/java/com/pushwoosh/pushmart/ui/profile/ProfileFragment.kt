package com.pushwoosh.pushmart.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pushwoosh.pushmart.R
import com.pushwoosh.pushmart.databinding.FragmentProfileBinding
import com.pushwoosh.pushmart.databinding.ViewProfileRowBinding
import com.pushwoosh.pushmart.services

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var binding: FragmentProfileBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentProfileBinding.bind(view)
        this.binding = binding
        row(
            binding.rowDetails,
            R.drawable.ic_account_circle,
            0xFFFF5A5F.toInt(),
            "Your details",
            "Name, email & member id")
        row(
            binding.rowWishlist,
            R.drawable.ic_heart,
            0xFFFF4D8D.toInt(),
            "Wishlist",
            "Products you saved")
        row(
            binding.rowPreferences,
            R.drawable.ic_tune,
            0xFFAF7BFF.toInt(),
            "Shopping preferences",
            "Brands and categories you love")
        row(
            binding.rowNotifications,
            R.drawable.ic_bell,
            0xFFFF8A3D.toInt(),
            "Offers & order updates",
            "Choose what PushMart sends you")
        row(
            binding.rowPlayground,
            R.drawable.ic_bolt,
            0xFF1476C6.toInt(),
            "Playground",
            "Post events and set tags")
        row(
            binding.rowInappShowcase,
            R.drawable.ic_sparkles,
            0xFFAF7BFF.toInt(),
            "In-app showcase",
            "Native in-app layouts, offline")
        row(
            binding.rowRichMedia,
            R.drawable.ic_star,
            0xFFFF8A3D.toInt(),
            "Rich media",
            "Display mode, style, server triggers")

        binding.rowDetails.root.setOnClickListener {
            findNavController().navigate(R.id.yourDetailsFragment)
        }
        binding.rowWishlist.root.setOnClickListener {
            findNavController().navigate(R.id.wishlistFragment)
        }
        binding.rowPreferences.root.setOnClickListener {
            findNavController().navigate(R.id.preferencesFragment)
        }
        binding.rowNotifications.root.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.rowPlayground.root.setOnClickListener {
            findNavController().navigate(R.id.playgroundFragment)
        }
        binding.rowInappShowcase.root.setOnClickListener {
            findNavController().navigate(R.id.inAppShowcaseFragment)
        }
        binding.rowRichMedia.root.setOnClickListener {
            findNavController().navigate(R.id.richMediaFragment)
        }

        binding.switchAccount.setOnClickListener {
            SwitchAccountSheet().show(parentFragmentManager, "switch-account")
        }
        binding.signOut.setOnClickListener {
            services.accountManager.signOut()
            val options = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.welcomeFragment, null, options)
        }
    }

    override fun onResume() {
        super.onResume()
        val binding = binding ?: return
        val account = services.account
        binding.avatar.text = account.initials
        binding.userName.text = account.userName.ifEmpty { "Guest" }
        binding.userEmail.text = account.userEmail.ifEmpty { "Not signed in" }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun row(
        row: ViewProfileRowBinding,
        icon: Int,
        tint: Int,
        title: String,
        subtitle: String
    ) {
        row.rowIcon.setImageResource(icon)
        row.rowIcon.setColorFilter(tint)
        row.rowTitle.text = title
        row.rowSubtitle.text = subtitle
    }
}
