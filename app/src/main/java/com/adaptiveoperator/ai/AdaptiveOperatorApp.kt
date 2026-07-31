package com.adaptiveoperator.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt wires the AI runtime, memory (Room), skills, and
 * security singletons from here down through every screen and service.
 *
 * Nothing in this class touches the network except, indirectly, the ModelManager's
 * download path -- see ai/runtime/ModelDownloader.kt. Inference itself is 100% local.
 */
@HiltAndroidApp
class AdaptiveOperatorApp : Application()
