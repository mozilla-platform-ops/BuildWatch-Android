package com.mozilla.buildwatch.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mozilla.buildwatch.util.openUrl
import com.mozilla.buildwatch.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val notificationsEnabled by vm.notificationsEnabled.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.setNotificationsEnabled(true) }

    val storedUsername by vm.username.collectAsStateWithLifecycle()
    // Show just the LDAP handle in the field; store full email internally.
    val displayHandle = storedUsername.substringBefore("@").takeIf { storedUsername.isNotEmpty() } ?: ""
    var fieldValue by remember(displayHandle) { mutableStateOf(displayHandle) }

    val appVersion = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "—"
        } catch (_: PackageManager.NameNotFoundException) { "—" }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Profile section
            SectionLabel("Profile")
            ListItem(
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                headlineContent = {
                    OutlinedTextField(
                        value = fieldValue,
                        onValueChange = { fieldValue = it },
                        label = { Text("LDAP Username") },
                        placeholder = { Text("rcurran") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            imeAction = ImeAction.Done,
                        ),
                        supportingText = { Text("Filters try pushes to your commits.") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                },
                // Commit on focus loss by tracking changes via onValueChange + a Done action
            )
            // Persist when field loses focus / user is done typing (save on any change after debounce
            // is handled by ViewModel; here we save on each keystroke for simplicity)
            if (fieldValue != displayHandle) {
                vm.setUsername(fieldValue)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Notifications section
            SectionLabel("Notifications")
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            val notificationStatus = when {
                notificationsEnabled -> "Enabled"
                hasPermission -> "Disabled"
                else -> "Not Set Up"
            }
            ListItem(
                headlineContent = { Text("Build Notifications") },
                supportingContent = { Text(notificationStatus) },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                vm.setNotificationsEnabled(false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setNotificationsEnabled(true)
                            }
                        },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // About section
            SectionLabel("About")
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text(appVersion) },
            )
            ListItem(
                headlineContent = { Text("TreeHerder") },
                trailingContent = {
                    IconButton(onClick = { openUrl(context, "https://treeherder.mozilla.org") }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open TreeHerder")
                    }
                },
            )
            ListItem(
                headlineContent = { Text("Taskcluster") },
                trailingContent = {
                    IconButton(onClick = { openUrl(context, "https://firefox-ci-tc.services.mozilla.com") }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open Taskcluster")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    )
}
