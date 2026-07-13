package com.likkai.linkrouter

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.ui.MainViewModel
import com.likkai.linkrouter.ui.SettingsViewModel
import com.likkai.linkrouter.ui.screens.AddEditRuleDialog
import com.likkai.linkrouter.ui.screens.BrowserPickerDialog
import com.likkai.linkrouter.ui.screens.CacheHistoryScreen
import com.likkai.linkrouter.ui.screens.FollowRedirectsScreen
import com.likkai.linkrouter.ui.screens.RuleListScreen
import com.likkai.linkrouter.ui.screens.SettingsScreen
import com.likkai.linkrouter.ui.theme.LinkRouterTheme

class MainActivity : ComponentActivity() {

    private var isDefaultBrowser by mutableStateOf(false)

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkDefaultBrowserStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkDefaultBrowserStatus()

        setContent {
            LinkRouterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = viewModel()

                    var showAddDialog by remember { mutableStateOf(false) }
                    var editingRule by remember { mutableStateOf<BrowserRule?>(null) }
                    var showDefaultPicker by remember { mutableStateOf(false) }

                    NavHost(
                        navController = navController,
                        startDestination = "rule_list",

                        // Material 3 Shared X-Axis transitions for smooth predictive back
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(400)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(400),
                                targetOffset = { it / 3 }
                            ) + fadeOut(animationSpec = tween(400))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(400),
                                initialOffset = { it / 3 }
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(400)
                            )
                        }
                    ) {
                        composable("rule_list") {
                            val rules by mainViewModel.rules.collectAsState()
                            val defaultBrowserLabel by mainViewModel.defaultBrowserLabel.collectAsState()
                            val debugMode by mainViewModel.debugMode.collectAsState()

                            RuleListScreen(
                                rules = rules,
                                defaultBrowserLabel = defaultBrowserLabel,
                                isDefaultBrowser = isDefaultBrowser,
                                debugMode = debugMode,
                                onSetDefault = { requestDefaultBrowser() },
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
                            "settings"
                        ) {
                            SettingsScreen(
                                onNavigateToFollowRedirects = { navController.navigate("follow_redirects") },
                                onNavigateToCacheHistory = { navController.navigate("cache_history") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "follow_redirects"
                        ) {
                            val settingsViewModel: SettingsViewModel = viewModel()
                            val followRedirects by settingsViewModel.followRedirects.collectAsState()
                            val redirectDomains by settingsViewModel.redirectDomains.collectAsState()

                            FollowRedirectsScreen(
                                followRedirects = followRedirects,
                                redirectDomains = redirectDomains,
                                onToggleFollowRedirects = { settingsViewModel.toggleFollowRedirects() },
                                onAddDomain = { domain -> settingsViewModel.addDomain(domain) },
                                onRemoveDomain = { domain -> settingsViewModel.removeDomain(domain) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "cache_history"
                        ) {
                            val settingsViewModel: SettingsViewModel = viewModel()
                            val cacheHistory by settingsViewModel.redirectCache.collectAsState()

                            CacheHistoryScreen(
                                cacheHistory = cacheHistory,
                                onDeleteCache = { cache -> settingsViewModel.deleteCacheEntry(cache) },
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
    }

    override fun onResume() {
        super.onResume()
        checkDefaultBrowserStatus()
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
}