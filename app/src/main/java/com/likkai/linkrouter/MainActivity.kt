package com.likkai.linkrouter

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.ui.MainViewModel
import com.likkai.linkrouter.ui.SettingsViewModel
import com.likkai.linkrouter.ui.screens.AddEditRuleDialog
import com.likkai.linkrouter.ui.screens.BrowserPickerDialog
import com.likkai.linkrouter.ui.screens.RuleListScreen
import com.likkai.linkrouter.ui.screens.SettingsScreen
import com.likkai.linkrouter.ui.theme.LinkRouterTheme

class MainActivity : ComponentActivity() {

    private var isDefaultBrowser by mutableStateOf(false)
    private var isBatteryOptimized by mutableStateOf(true)

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkDefaultBrowserStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkDefaultBrowserStatus()
        checkBatteryOptimization()

        setContent {
            LinkRouterTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                val activity = LocalActivity.current
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                var showAddDialog by remember { mutableStateOf(false) }
                var editingRule by remember { mutableStateOf<BrowserRule?>(null) }
                var showDefaultPicker by remember { mutableStateOf(false) }

                BackHandler {
                    if (currentRoute == "rule_list") {
                        activity?.moveTaskToBack(true)
                    } else {
                        navController.popBackStack()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "rule_list",

                    // Rules screen stays in place — no enter/exit animation
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable("rule_list") {
                        val rules by mainViewModel.rules.collectAsState()
                        val defaultBrowserLabel by mainViewModel.defaultBrowserLabel.collectAsState()
                        val debugMode by mainViewModel.debugMode.collectAsState()

                        RuleListScreen(
                            rules = rules,
                            defaultBrowserLabel = defaultBrowserLabel,
                            isDefaultBrowser = isDefaultBrowser,
                            isBatteryOptimized = isBatteryOptimized,
                            debugMode = debugMode,
                            onSetDefault = { requestDefaultBrowser() },
                            onRequestBatteryOptimization = { requestBatteryOptimization() },
                            onAddRule = { showAddDialog = true },
                            onEditRule = { rule -> editingRule = rule },
                            onDeleteRule = { rule -> mainViewModel.deleteRule(rule) },
                            onToggleRule = { rule -> mainViewModel.toggleRule(rule) },
                            onMoveUp = { rule -> mainViewModel.moveRuleUp(rule, rules) },
                            onMoveDown = { rule -> mainViewModel.moveRuleDown(rule, rules) },
                            onChangeDefaultBrowser = { showDefaultPicker = true },
                            onToggleDebugMode = { mainViewModel.toggleDebugMode() },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }

                    composable(
                        "settings",
                        enterTransition = {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                        },
                        exitTransition = {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
                        },
                        popEnterTransition = {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End)
                        },
                        popExitTransition = {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
                        }
                    ) {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        val followRedirects by settingsViewModel.followRedirects.collectAsState()
                        val redirectDomains by settingsViewModel.redirectDomains.collectAsState()

                        SettingsScreen(
                            followRedirects = followRedirects,
                            redirectDomains = redirectDomains,
                            onToggleFollowRedirects = { settingsViewModel.toggleFollowRedirects() },
                            onAddDomain = { domain -> settingsViewModel.addDomain(domain) },
                            onRemoveDomain = { domain -> settingsViewModel.removeDomain(domain) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // Add rule dialog
                if (showAddDialog) {
                    val browsers by mainViewModel.installedBrowsers.collectAsState()

                    AddEditRuleDialog(
                        editingRule = null,
                        installedBrowsers = browsers,
                        onDismiss = { showAddDialog = false },
                        onSave = { matchType, pattern, browser ->
                            mainViewModel.addRule(matchType, pattern, browser)
                            showAddDialog = false
                        },
                        onUpdate = { }
                    )
                }

                // Edit rule dialog
                editingRule?.let { rule ->
                    val browsers by mainViewModel.installedBrowsers.collectAsState()

                    AddEditRuleDialog(
                        editingRule = rule,
                        installedBrowsers = browsers,
                        onDismiss = { editingRule = null },
                        onSave = { _, _, _ -> },
                        onUpdate = { updatedRule ->
                            mainViewModel.updateRule(updatedRule)
                            editingRule = null
                        }
                    )
                }

                // Default browser picker
                if (showDefaultPicker) {
                    val browsers by mainViewModel.installedBrowsers.collectAsState()

                    BrowserPickerDialog(
                        browsers = browsers,
                        selectedPackage = mainViewModel.defaultBrowserPackage.collectAsState().value,
                        onSelect = { browser ->
                            mainViewModel.setDefaultBrowser(browser)
                            showDefaultPicker = false
                        },
                        onDismiss = { showDefaultPicker = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultBrowserStatus()
        checkBatteryOptimization()
    }

    private fun checkDefaultBrowserStatus() {
        val roleManager = getSystemService(RoleManager::class.java)
        isDefaultBrowser = roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
    }

    private fun requestDefaultBrowser() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
            requestRoleLauncher.launch(intent)
        }
    }

    private fun checkBatteryOptimization() {
        val pm = getSystemService(PowerManager::class.java)
        isBatteryOptimized = pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}