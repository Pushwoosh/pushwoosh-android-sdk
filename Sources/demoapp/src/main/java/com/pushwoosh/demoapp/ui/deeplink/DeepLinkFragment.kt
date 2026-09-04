package com.pushwoosh.demoapp.ui.deeplink

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.pushwoosh.sampleapp.databinding.FragmentDeepLinkBinding

/**
 * Shows the deep link URI that brought the user here, plus its host / path / query breakdown.
 *
 * The URI arrives as a plain string argument under [ARG_URI]: Safe Args is not applied in this
 * sample, so the caller builds the Bundle by hand.
 *
 * @see com.pushwoosh.demoapp.MainActivity
 */
class DeepLinkFragment : Fragment() {

    private var binding: FragmentDeepLinkBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeepLinkBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The bottom nav is hidden on this destination, so nothing else keeps content off the
        // gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }

        val raw = requireArguments().getString(ARG_URI).orEmpty()
        val uri = Uri.parse(raw)

        binding?.deepLinkUri?.text = raw
        binding?.deepLinkParts?.text =
            DeepLinkBreakdown.rows(uri.host, uri.path, queryParamsOf(uri)).joinToString("\n")
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun queryParamsOf(uri: Uri): List<Pair<String, String>> {
        // getQueryParameter throws UnsupportedOperationException on opaque URIs such as
        // "pwdemo:demo".
        if (!uri.isHierarchical) {
            return emptyList()
        }
        return uri.queryParameterNames.flatMap { name ->
            uri.getQueryParameters(name).map { value -> name to value }
        }
    }

    companion object {
        const val ARG_URI = "uri"
    }
}
