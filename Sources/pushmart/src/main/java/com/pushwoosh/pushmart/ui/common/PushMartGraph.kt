package com.pushwoosh.pushmart.ui.common

import androidx.navigation.NavController
import com.pushwoosh.pushmart.R

/**
 * Sets the PushMart navigation graph on this controller. It starts at Home when [onboarded] and at
 * Onboarding otherwise. This is the only place that decides the start destination: the graph XML
 * declares none.
 */
fun NavController.setPushMartGraph(onboarded: Boolean) {
    val graph = navInflater.inflate(R.navigation.nav_graph)
    graph.setStartDestination(if (onboarded) R.id.homeFragment else R.id.onboardingFragment)
    setGraph(graph, null)
}
