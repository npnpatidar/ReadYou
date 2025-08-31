package me.ash.reader.ui.page.settings.ai

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.ash.reader.R
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.base.RYTextField2
import me.ash.reader.ui.component.base.Subtitle
import me.ash.reader.ui.theme.palette.onLight

@Composable
fun AiCredentialsPage(
    onBack: () -> Unit,
    viewModel: AiCredentialsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val apiKeyState = rememberTextFieldState(uiState.credentials.apiKey)
    val baseUrlState = rememberTextFieldState(uiState.credentials.baseUrl)
    val timeoutState = rememberTextFieldState(uiState.credentials.timeout)
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.credentials) {
        if (apiKeyState.text.toString() != uiState.credentials.apiKey) apiKeyState.setTextAndPlaceCursorAtEnd(uiState.credentials.apiKey)
        if (baseUrlState.text.toString() != uiState.credentials.baseUrl) baseUrlState.setTextAndPlaceCursorAtEnd(uiState.credentials.baseUrl)
        if (timeoutState.text.toString() != uiState.credentials.timeout) timeoutState.setTextAndPlaceCursorAtEnd(uiState.credentials.timeout)
    }

    LaunchedEffect(apiKeyState.text) {
        viewModel.updateApiKey(apiKeyState.text.toString())
    }

    LaunchedEffect(baseUrlState.text) { viewModel.updateBaseUrl(baseUrlState.text.toString()) }
    LaunchedEffect(timeoutState.text) { viewModel.updateTimeout(timeoutState.text.toString()) }

    RYScaffold(
        containerColor = MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack
            )
        },
        content = {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    DisplayText(text = "AI Credentials", desc = "")
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Subtitle(text = "API Details")
                        RYTextField2(
                            state = apiKeyState,
                            label = "API Key",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        RYTextField2(
                            state = baseUrlState,
                            label = "Base URL",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        RYTextField2(
                            state = timeoutState,
                            label = "Timeout (seconds)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    modelDropdownExpanded = !modelDropdownExpanded
                                    Log.d("AiCredentialsPage", "Dropdown expanded: $modelDropdownExpanded")
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.credentials.modelId,
                                onValueChange = {},
                                label = { Text("Model ID (${uiState.models.size} available)") },
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = modelDropdownExpanded,
                                onDismissRequest = { modelDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (uiState.models.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.isLoadingModels) stringResource(R.string.loading) else "No models found") },
                                        onClick = { modelDropdownExpanded = false },
                                        enabled = false,
                                    )
                                } else {
                                    uiState.models.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                viewModel.updateModelId(model)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    )
}
