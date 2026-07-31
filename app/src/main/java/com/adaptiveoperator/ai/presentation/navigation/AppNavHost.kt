package com.adaptiveoperator.ai.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adaptiveoperator.ai.presentation.chat.ChatScreen
import com.adaptiveoperator.ai.presentation.home.HomeScreen
import com.adaptiveoperator.ai.presentation.memory.MemoryScreen
import com.adaptiveoperator.ai.presentation.modelmanager.ModelManagerScreen
import com.adaptiveoperator.ai.presentation.operator.OperatorScreen
import com.adaptiveoperator.ai.presentation.security.SecurityCenterScreen
import com.adaptiveoperator.ai.presentation.settings.SettingsScreen
import com.adaptiveoperator.ai.presentation.skills.SkillLibraryScreen

private data class BottomTab(val destination: Destination, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Destination.Home, "Home", Icons.Filled.Home),
    BottomTab(Destination.Chat, "Chat", Icons.Filled.Chat),
    BottomTab(Destination.Operator, "Operator", Icons.Filled.SmartToy),
    BottomTab(Destination.Memory, "Memory", Icons.Filled.Memory),
    BottomTab(Destination.Security, "Security", Icons.Filled.Security),
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true,
                        onClick = {
                            navController.navigate(tab.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onOpenModelManager = { navController.navigate(Destination.ModelManager.route) },
                    onOpenOperator = { navController.navigate(Destination.Operator.route) },
                    onOpenSkills = { navController.navigate(Destination.Skills.route) },
                    onOpenSettings = { navController.navigate(Destination.Settings.route) },
                )
            }
            composable(Destination.Chat.route) { ChatScreen() }
            composable(Destination.Operator.route) { OperatorScreen() }
            composable(Destination.ModelManager.route) { ModelManagerScreen() }
            composable(Destination.Skills.route) { SkillLibraryScreen() }
            composable(Destination.Memory.route) { MemoryScreen() }
            composable(Destination.Security.route) { SecurityCenterScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
        }
    }
}
