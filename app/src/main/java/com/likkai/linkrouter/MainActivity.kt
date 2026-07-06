package com.likkai.linkrouter

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.likkai.linkrouter.browser.BrowserApp
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.ui.MainViewModel
import com.likkai.linkrouter.ui.screens.AddEditRuleDialog
import com.likkai.linkrouter.ui.screens.BrowserPickerDialog
import com.likkai.linkrouter.ui.screens.RuleListScreen
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
                val viewModel: MainViewModel = viewModel()
                val rules by viewModel.rules.collectAsState()
                val browsers by viewModel.installedBrowsers.collectAsState()
                val defaultBrowserLabel by viewModel.defaultBrowserLabel.collectAsState()
                val debugMode by viewModel.debugMode.collectAsState()

                var showAddDialog by remember { mutableStateOf(false) }
                var editingRule by remember { mutableStateOf<BrowserRule?>(null) }
                var showDefaultPicker by remember { mutableStateOf(false) }

                RuleListScreen(
                    rules = rules,
                    defaultBrowserLabel = defaultBrowserLabel,
                    isDefaultBrowser = isDefaultBrowser,
                    debugMode = debugMode,
                    onSetDefault = { requestDefaultBrowser() },
                    onAddRule = { showAddDialog = true },
                    onEditRule = { rule -> editingRule = rule },
                    onDeleteRule = { rule -> viewModel.deleteRule(rule) },
                    onToggleRule = { rule -> viewModel.toggleRule(rule) },
                    onMoveUp = { rule -> viewModel.moveRuleUp(rule, rules) },
                    onMoveDown = { rule -> viewModel.moveRuleDown(rule, rules) },
                    onChangeDefaultBrowser = { showDefaultPicker = true },
                    onToggleDebugMode = { viewModel.toggleDebugMode() }
                )

                // Add rule dialog
                if (showAddDialog) {
                    AddEditRuleDialog(
                        editingRule = null,
                        installedBrowsers = browsers,
                        onDismiss = { showAddDialog = false },
                        onSave = { matchType, pattern, browser ->
                            viewModel.addRule(matchType, pattern, browser)
                            showAddDialog = false
                        },
                        onUpdate = { }
                    )
                }

                // Edit rule dialog
                editingRule?.let { rule ->
                    AddEditRuleDialog(
                        editingRule = rule,
                        installedBrowsers = browsers,
                        onDismiss = { editingRule = null },
                        onSave = { _, _, _ -> },
                        onUpdate = { updatedRule ->
                            viewModel.updateRule(updatedRule)
                            editingRule = null
                        }
                    )
                }

                // Default browser picker
                if (showDefaultPicker) {
                    BrowserPickerDialog(
                        browsers = browsers,
                        selectedPackage = viewModel.defaultBrowserPackage.collectAsState().value,
                        onSelect = { browser ->
                            viewModel.setDefaultBrowser(browser)
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