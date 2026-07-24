package com.pushwoosh.inapp.ui.activity

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

class InAppOverlayActivityManifestTest {

    /// Verifies the overlay Activity opts out of recreation on common configuration changes,
    /// keeping the shown view intact (no rebuild/animation replay). Correctness on an actual
    /// recreate is guarded in the Activity itself (savedInstanceState +
    /// isChangingConfigurations). Robolectric does not register library activities in its
    /// PackageManager, so the manifest is parsed directly.
    @Test
    fun overlayActivityHandlesConfigChangesWithoutRecreate() {
        val manifest = findModuleManifest()
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val activities = document.getElementsByTagName("activity")

        var overlayActivity: Element? = null
        for (i in 0 until activities.length) {
            val element = activities.item(i) as Element
            if (element.getAttribute("android:name") == InAppOverlayActivity::class.java.name) {
                overlayActivity = element
            }
        }
        assertNotNull("InAppOverlayActivity not declared in module manifest", overlayActivity)

        val declared = overlayActivity!!.getAttribute("android:configChanges").split("|").toSet()
        val required = setOf(
            "orientation", "screenSize", "smallestScreenSize", "screenLayout",
            "keyboardHidden", "density", "uiMode"
        )
        assertEquals(required, required.intersect(declared))
    }

    private fun findModuleManifest(): File {
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null) {
            val candidate = File(dir, "pushwoosh-inapp-ui/src/main/AndroidManifest.xml")
            if (candidate.exists()) return candidate
            val local = File(dir, "src/main/AndroidManifest.xml")
            if (local.exists() && dir.name == "pushwoosh-inapp-ui") return local
            dir = dir.parentFile
        }
        throw AssertionError("module AndroidManifest.xml not found from " + System.getProperty("user.dir"))
    }
}
