package com.spywer.yoho.ui.account

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.spywer.yoho.ui.components.YohoBackButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.spywer.yoho.R
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics

@Composable
fun AccountScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AccountViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    var registerMode by rememberSaveable { mutableStateOf(false) }
    val titleSize = (metrics.sectionTitleSize.value + 4f).sp
    val bodySize = (metrics.searchTextSize.value + 2f).sp

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = metrics.screenPadding,
                vertical = metrics.sectionGap,
            ),
    ) {
        when {
            state.loading -> {
                Text(
                    text = stringResource(R.string.nav_account),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = titleSize,
                    lineHeight = metrics.lineOf(titleSize),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(metrics.sectionGap))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.user != null -> {
                Text(
                    text = stringResource(R.string.nav_account),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = titleSize,
                    lineHeight = metrics.lineOf(titleSize),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(metrics.sectionGap))
                val user = state.user!!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (!user.avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    } else {
                        Box(
                            Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (user.resolvedDisplayName() ?: "?")
                                    .take(1)
                                    .uppercase(),
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(metrics.sectionGap))
                    Text(
                        text = user.resolvedDisplayName().orEmpty(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (metrics.cardTitleSize.value + 4f).sp,
                        lineHeight = metrics.lineOf((metrics.cardTitleSize.value + 4f).sp),
                    )
                    if (!user.username.isNullOrBlank()) {
                        Text(
                            text = "@${user.username}${user.userCode.orEmpty()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = bodySize,
                            lineHeight = metrics.lineOf(bodySize),
                        )
                    }
                    if (!user.email.isNullOrBlank()) {
                        Text(
                            text = user.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = bodySize,
                            lineHeight = metrics.lineOf(bodySize),
                            modifier = Modifier.padding(top = metrics.blockGap),
                        )
                    }
                    Spacer(Modifier.height(if (metrics.isTablet) 28.dp else metrics.sectionGap * 2))
                    OutlinedButton(
                        onClick = viewModel::signOut,
                        enabled = !state.busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (metrics.isTablet) 52.dp else metrics.searchHeight),
                    ) {
                        Text(
                            stringResource(R.string.account_sign_out),
                            fontSize = bodySize,
                            lineHeight = metrics.lineOf(bodySize),
                        )
                    }
                }
            }

            else -> {
                AuthForm(
                    metrics = metrics,
                    titleSize = titleSize,
                    registerMode = registerMode,
                    busy = state.busy,
                    error = state.error,
                    onBack = {
                        if (registerMode) {
                            registerMode = false
                        } else {
                            onBack?.invoke()
                        }
                    },
                    onToggleMode = { registerMode = !registerMode },
                    onSignIn = viewModel::signIn,
                    onSignUp = viewModel::signUp,
                )
            }
        }
    }
}

@Composable
private fun AuthForm(
    metrics: HomeMetrics,
    titleSize: TextUnit,
    registerMode: Boolean,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    val fieldText = (metrics.searchTextSize.value + 2f).sp
    val hintText = metrics.searchTextSize
    val fieldStyle = TextStyle(
        fontSize = fieldText,
        lineHeight = metrics.lineOf(fieldText),
    )

    val emailErr = when {
        !showErrors -> null
        email.isBlank() -> stringResource(R.string.account_error_email_blank)
        !email.isValidEmail() -> stringResource(R.string.account_error_email_invalid)
        else -> null
    }
    val passwordErr = when {
        !showErrors -> null
        password.isBlank() -> stringResource(R.string.account_error_password_blank)
        password.length < 8 -> stringResource(R.string.account_error_password_short)
        else -> null
    }
    val usernameErr = when {
        !registerMode || !showErrors -> null
        username.isBlank() -> stringResource(R.string.account_error_username_blank)
        else -> null
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    fun formValid(): Boolean {
        if (!email.isValidEmail() || password.length < 8) return false
        if (registerMode && username.isBlank()) return false
        return true
    }

    fun submit() {
        showErrors = true
        if (!formValid() || busy) return
        if (registerMode) {
            onSignUp(email, password, username, displayName)
        } else {
            onSignIn(email, password)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(metrics.sectionGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    if (registerMode) R.string.account_sign_up
                    else R.string.account_sign_in,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            YohoBackButton(
                onClick = {
                    if (!busy) {
                        showErrors = false
                        onBack()
                    }
                },
            )
        }
        Text(
            text = stringResource(
                if (registerMode) R.string.account_sign_up_hint
                else R.string.account_sign_in_hint,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = hintText,
            lineHeight = metrics.lineOf(hintText),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.account_email), fontSize = hintText) },
            singleLine = true,
            enabled = !busy,
            supportingText = emailErr?.let {
                {
                    Text(it, fontSize = hintText, color = MaterialTheme.colorScheme.error)
                }
            },
            textStyle = fieldStyle,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (registerMode) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filter { ch -> !ch.isWhitespace() } },
                label = { Text(stringResource(R.string.account_username), fontSize = hintText) },
                singleLine = true,
                enabled = !busy,
                supportingText = usernameErr?.let {
                    {
                        Text(it, fontSize = hintText, color = MaterialTheme.colorScheme.error)
                    }
                },
                textStyle = fieldStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.account_display_name), fontSize = hintText) },
                singleLine = true,
                enabled = !busy,
                textStyle = fieldStyle,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.account_password), fontSize = hintText) },
            singleLine = true,
            enabled = !busy,
            supportingText = passwordErr?.let {
                {
                    Text(it, fontSize = hintText, color = MaterialTheme.colorScheme.error)
                }
            },
            textStyle = fieldStyle,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!error.isNullOrBlank()) {
            AuthErrorBanner(message = error, textSize = hintText)
        }
        Button(
            onClick = { submit() },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(
                        if (registerMode) R.string.account_sign_up
                        else R.string.account_sign_in,
                    ),
                    fontSize = fieldText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (!registerMode) {
            TextButton(
                onClick = {
                    showErrors = false
                    onToggleMode()
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.account_need_account),
                    fontSize = hintText,
                )
            }
        }
    }
}

@Composable
private fun AuthErrorBanner(
    message: String,
    textSize: TextUnit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun String.isValidEmail(): Boolean {
    val value = trim()
    return value.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(value).matches()
}
