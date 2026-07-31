package com.adaptiveoperator.ai.presentation.navigation

/**
 * Top-level screens, mirroring Section 63's Presentation module:
 * Home, Chat, Operator, Floating Operator (settings only -- the bubble itself is an
 * overlay Service, not a nav destination), Skills, Memory, Model Manager,
 * Runtime Monitor, Security, Settings.
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Chat : Destination("chat")
    data object Operator : Destination("operator")
    data object ModelManager : Destination("model_manager")
    data object Skills : Destination("skills")
    data object Memory : Destination("memory")
    data object Security : Destination("security")
    data object Settings : Destination("settings")
}
