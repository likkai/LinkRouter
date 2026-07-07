package com.likkai.linkrouter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import com.likkai.linkrouter.ui.components.BrowserIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.likkai.linkrouter.browser.BrowserApp
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.data.MatchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    editingRule: BrowserRule?,
    installedBrowsers: List<BrowserApp>,
    onDismiss: () -> Unit,
    onSave: (MatchType, String, BrowserApp) -> Unit,
    onUpdate: (BrowserRule) -> Unit
) {
    var matchType by remember {
        mutableStateOf(editingRule?.matchType ?: MatchType.DOMAIN)
    }
    var pattern by remember {
        mutableStateOf(editingRule?.pattern ?: "")
    }
    var selectedBrowser by remember {
        mutableStateOf(
            editingRule?.let { rule ->
                installedBrowsers.find { it.packageName == rule.targetBrowserPackage }
            }
        )
    }
    var showBrowserPicker by remember { mutableStateOf(false) }

    val isEditing = editingRule != null
    val canSave = pattern.isNotBlank() && selectedBrowser != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "Edit Rule" else "Add Rule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Match type selector
                Text(
                    "Match Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = matchType == MatchType.DOMAIN,
                        onClick = { matchType = MatchType.DOMAIN },
                        label = { Text("Entire Domain") }
                    )
                    FilterChip(
                        selected = matchType == MatchType.EXACT_URL,
                        onClick = { matchType = MatchType.EXACT_URL },
                        label = { Text("Exact URL") }
                    )
                }

                // Pattern input
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = {
                        Text(
                            when (matchType) {
                                MatchType.EXACT_URL -> "URL"
                                MatchType.DOMAIN -> "Domain"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (matchType) {
                                MatchType.EXACT_URL -> "https://twitter.com/home"
                                MatchType.DOMAIN -> "twitter.com"
                            }
                        )
                    },
                    supportingText = {
                        Text(
                            when (matchType) {
                                MatchType.EXACT_URL -> "Match this exact URL only"
                                MatchType.DOMAIN -> "Match all URLs on this domain (including subdomains)"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                // Browser selector
                Text(
                    "Open with",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBrowserPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedBrowser != null) {
                            BrowserIcon(
                                packageName = selectedBrowser!!.packageName,
                                size = 28.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            selectedBrowser?.label ?: "Select a browser",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedBrowser != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedBrowser?.let { browser ->
                        if (isEditing && editingRule != null) {
                            onUpdate(
                                editingRule.copy(
                                    matchType = matchType,
                                    pattern = pattern.trim(),
                                    targetBrowserPackage = browser.packageName,
                                    targetBrowserLabel = browser.label
                                )
                            )
                        } else {
                            onSave(matchType, pattern, browser)
                        }
                    }
                },
                enabled = canSave
            ) {
                Text(if (isEditing) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Browser picker sub-dialog
    if (showBrowserPicker) {
        BrowserPickerDialog(
            browsers = installedBrowsers,
            selectedPackage = selectedBrowser?.packageName,
            onSelect = { browser ->
                selectedBrowser = browser
                showBrowserPicker = false
            },
            onDismiss = { showBrowserPicker = false }
        )
    }
}
