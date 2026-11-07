package com.spywer.yoho.ui.account

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.spywer.yoho.BuildConfig
import com.spywer.yoho.R
import com.spywer.yoho.data.AppSettings
import com.spywer.yoho.data.remote.AuthUserDto
import com.spywer.yoho.ui.components.YohoBackButton
import com.spywer.yoho.ui.layout.HomeMetrics
import com.spywer.yoho.ui.layout.rememberHomeMetrics
import com.spywer.yoho.ui.legal.LegalDocument
import com.spywer.yoho.ui.legal.LegalDocumentActivity
import com.spywer.yoho.ui.support.SupportProjectDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AccountSubPage {
    Friends,
    Messages,
    Settings,
    About,
    AboutApp,
}

@Composable
fun AccountScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AccountViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = rememberHomeMetrics()
    var registerMode by rememberSaveable { mutableStateOf(false) }
    // Не saveable-enum: после добавления AboutApp восстановление могло ронять экран.
    var subPage by remember { mutableStateOf<AccountSubPage?>(null) }
    val titleSize = (metrics.sectionTitleSize.value + 4f).sp
    val bodySize = (metrics.searchTextSize.value + 2f).sp
    val onAuthForm = state.user == null && !state.loading

    BackHandler(enabled = subPage != null || (onAuthForm && (registerMode || onBack != null))) {
        when {
            subPage != null -> subPage = null
            registerMode -> registerMode = false
            else -> onBack?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    when (val page = subPage) {
        AccountSubPage.Friends -> AccountSocialSoonPage(
            title = stringResource(R.string.account_friends),
            metrics = metrics,
            titleSize = titleSize,
            bodySize = bodySize,
            onBack = { subPage = null },
        )
        AccountSubPage.Messages -> AccountSocialSoonPage(
            title = stringResource(R.string.account_messages),
            metrics = metrics,
            titleSize = titleSize,
            bodySize = bodySize,
            onBack = { subPage = null },
        )
        AccountSubPage.Settings -> AccountSettingsPage(
            metrics = metrics,
            titleSize = titleSize,
            bodySize = bodySize,
            onBack = { subPage = null },
        )
        AccountSubPage.About -> AccountAboutServicePage(
            metrics = metrics,
            titleSize = titleSize,
            bodySize = bodySize,
            onBack = { subPage = null },
        )
        AccountSubPage.AboutApp -> AccountAboutAppPage(
            metrics = metrics,
            titleSize = titleSize,
            bodySize = bodySize,
            onBack = { subPage = null },
        )
        null -> Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = metrics.screenPadding,
                vertical = metrics.sectionGap,
            )
            .padding(bottom = 40.dp),
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
                AccountProfileHeader(
                    user = state.user!!,
                    metrics = metrics,
                    bodySize = bodySize,
                    busy = state.busy,
                    onSignOut = viewModel::signOut,
                )
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
        Spacer(Modifier.height(if (metrics.isTablet) 48.dp else 36.dp))
        AccountMenuGrid(
            metrics = metrics,
            onFriends = { subPage = AccountSubPage.Friends },
            onMessages = { subPage = AccountSubPage.Messages },
            onSettings = { subPage = AccountSubPage.Settings },
            onAbout = { subPage = AccountSubPage.About },
            onAboutApp = { subPage = AccountSubPage.AboutApp },
        )
    }
        Text(
            text = stringResource(
                R.string.account_app_version,
                stringResource(R.string.app_name),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toString(),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = metrics.cardMetaSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = metrics.screenPadding,
                    vertical = metrics.sectionGap,
                ),
        )
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountProfileHeader(
    user: AuthUserDto,
    metrics: HomeMetrics,
    bodySize: TextUnit,
    busy: Boolean,
    onSignOut: () -> Unit,
) {
    val metaSize = metrics.cardMetaSize
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = "@${user.username}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = bodySize,
                    lineHeight = metrics.lineOf(bodySize),
                    fontWeight = FontWeight.Medium,
                )
                user.resolvedUserCode()?.let { code ->
                    Text(
                        text = " $code",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = bodySize,
                        lineHeight = metrics.lineOf(bodySize),
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = metrics.blockGap),
        ) {
            user.role?.takeIf { it.isNotBlank() }?.let { role ->
                Text(
                    text = roleLabel(role),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metaSize,
                    lineHeight = metaSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            val metaParts = buildList {
                formatRegisteredDate(user.registeredAt)?.let { date ->
                    add(stringResource(R.string.account_registered_since, date))
                }
                val following = user.followingCount ?: 0
                val followers = user.followersCount ?: 0
                add(
                    pluralStringResource(
                        R.plurals.account_following,
                        following,
                        following,
                    ),
                )
                add(
                    pluralStringResource(
                        R.plurals.account_followers,
                        followers,
                        followers,
                    ),
                )
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = metaSize,
                    lineHeight = metaSize,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (!user.email.isNullOrBlank()) {
            Text(
                text = user.email,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = metaSize,
                lineHeight = metrics.lineOf(metaSize),
                modifier = Modifier.padding(top = metrics.blockGap),
            )
        }
        Spacer(Modifier.height(if (metrics.isTablet) 28.dp else metrics.sectionGap * 2))
        OutlinedButton(
            onClick = onSignOut,
            enabled = !busy,
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

@Composable
private fun roleLabel(role: String): String =
    when (role.lowercase()) {
        "moderator", "mod" -> stringResource(R.string.account_role_moderator)
        "admin" -> stringResource(R.string.account_role_admin)
        "streamer" -> stringResource(R.string.account_role_streamer)
        else -> stringResource(R.string.account_role_user)
    }

private fun formatRegisteredDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val date = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() ?: return null
    return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy 'г'.", Locale("ru")))
}

@Composable
private fun AccountMenuGrid(
    metrics: HomeMetrics,
    onFriends: () -> Unit,
    onMessages: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onAboutApp: () -> Unit,
) {
    val tileHeight = if (metrics.isTablet) 112.dp else 96.dp
    val gap = metrics.cardGap
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            AccountMenuTile(
                icon = Icons.Outlined.People,
                label = stringResource(R.string.account_friends),
                textSize = metrics.cardMetaSize,
                onClick = onFriends,
                modifier = Modifier.weight(1f).height(tileHeight),
            )
            AccountMenuTile(
                icon = Icons.Outlined.Chat,
                label = stringResource(R.string.account_messages),
                textSize = metrics.cardMetaSize,
                onClick = onMessages,
                modifier = Modifier.weight(1f).height(tileHeight),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            AccountMenuTile(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.account_settings),
                textSize = metrics.cardMetaSize,
                onClick = onSettings,
                modifier = Modifier.weight(1f).height(tileHeight),
            )
            AccountMenuTile(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.account_about),
                textSize = metrics.cardMetaSize,
                onClick = onAbout,
                modifier = Modifier.weight(1f).height(tileHeight),
            )
        }
        AccountMenuTile(
            icon = Icons.Outlined.Apps,
            label = stringResource(R.string.account_about_app),
            textSize = metrics.cardMetaSize,
            onClick = onAboutApp,
            modifier = Modifier.fillMaxWidth().height(tileHeight),
        )
    }
}

@Composable
private fun AccountSocialSoonPage(
    title: String,
    metrics: HomeMetrics,
    titleSize: TextUnit,
    bodySize: TextUnit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = metrics.screenPadding, vertical = metrics.sectionGap),
    ) {
        AccountSubPageHeader(
            title = title,
            titleSize = titleSize,
            gap = metrics.sectionGap,
            onBack = onBack,
            showBack = metrics.isTablet,
        )
        Spacer(Modifier.height(metrics.sectionGap))
        Text(
            text = stringResource(R.string.account_social_soon),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = bodySize,
            lineHeight = metrics.lineOf(bodySize),
        )
    }
}

@Composable
private fun AccountMenuTile(
    icon: ImageVector,
    label: String,
    textSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (textSize.value >= 16f) 36.dp else 32.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun AccountSettingsPage(
    metrics: HomeMetrics,
    titleSize: TextUnit,
    bodySize: TextUnit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var pipEnabled by remember {
        mutableStateOf(AppSettings.pipEnabled(context))
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = metrics.screenPadding, vertical = metrics.sectionGap),
    ) {
        AccountSubPageHeader(
            title = stringResource(R.string.account_settings),
            titleSize = titleSize,
            gap = metrics.sectionGap,
            onBack = onBack,
            showBack = metrics.isTablet,
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    val next = !pipEnabled
                    pipEnabled = next
                    AppSettings.setPipEnabled(context, next)
                }
                .padding(
                    horizontal = if (metrics.isTablet) 20.dp else 16.dp,
                    vertical = if (metrics.isTablet) 20.dp else 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = stringResource(R.string.settings_pip),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = (bodySize.value + 2f).sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_pip_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (metrics.cardMetaSize.value + 2f).sp,
                    lineHeight = metrics.lineOf((metrics.cardMetaSize.value + 2f).sp),
                )
            }
            Switch(
                checked = pipEnabled,
                onCheckedChange = { checked ->
                    pipEnabled = checked
                    AppSettings.setPipEnabled(context, checked)
                },
            )
        }
    }
}

@Composable
private fun AccountAboutServicePage(
    metrics: HomeMetrics,
    titleSize: TextUnit,
    bodySize: TextUnit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.screenPadding, vertical = metrics.sectionGap),
    ) {
        AccountSubPageHeader(
            title = stringResource(R.string.account_about),
            titleSize = titleSize,
            gap = metrics.sectionGap,
            onBack = onBack,
            showBack = metrics.isTablet,
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        Text(
            text = stringResource(R.string.account_about_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = bodySize,
            lineHeight = metrics.lineOf(bodySize),
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        LegalLinkRow(
            label = stringResource(R.string.legal_terms_title),
            textSize = bodySize,
            onClick = {
                context.startActivity(LegalDocumentActivity.intent(context, LegalDocument.Terms))
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        LegalLinkRow(
            label = stringResource(R.string.legal_privacy_title),
            textSize = bodySize,
            onClick = {
                context.startActivity(LegalDocumentActivity.intent(context, LegalDocument.Privacy))
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        LegalLinkRow(
            label = stringResource(R.string.legal_dmca_title),
            textSize = bodySize,
            onClick = {
                context.startActivity(LegalDocumentActivity.intent(context, LegalDocument.Dmca))
            },
        )
    }
}

@Composable
private fun AccountAboutAppPage(
    metrics: HomeMetrics,
    titleSize: TextUnit,
    bodySize: TextUnit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showSupportDialog by rememberSaveable { mutableStateOf(false) }
    if (showSupportDialog) {
        SupportProjectDialog(onDismiss = { showSupportDialog = false })
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.screenPadding, vertical = metrics.sectionGap),
    ) {
        AccountSubPageHeader(
            title = stringResource(R.string.account_about_app),
            titleSize = titleSize,
            gap = metrics.sectionGap,
            onBack = onBack,
            showBack = metrics.isTablet,
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        Image(
            painter = painterResource(R.drawable.ic_launcher_fg),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(if (metrics.isTablet) 96.dp else 80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1A2026)),
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        Text(
            text = stringResource(
                R.string.account_about_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toString(),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = bodySize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(metrics.sectionGap * 2))
        Text(
            text = stringResource(R.string.account_about_dev),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = bodySize,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(metrics.sectionGap))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(metrics.cardGap),
        ) {
            DevContactChip(
                icon = Icons.AutoMirrored.Outlined.Send,
                label = stringResource(R.string.account_about_dev_telegram),
                value = stringResource(R.string.account_about_dev_telegram_value),
                textSize = metrics.cardMetaSize,
                compact = !metrics.isTablet,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(context.getString(R.string.account_about_dev_telegram_url)),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            DevContactChip(
                icon = Icons.Outlined.Email,
                label = stringResource(R.string.account_about_dev_email),
                value = stringResource(R.string.account_about_dev_email_value),
                textSize = metrics.cardMetaSize,
                compact = !metrics.isTablet,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse(context.getString(R.string.account_about_dev_email_uri)),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(metrics.sectionGap))
        Button(
            onClick = { showSupportDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (metrics.isTablet) 52.dp else metrics.searchHeight),
        ) {
            Text(
                stringResource(R.string.support_development),
                fontSize = bodySize,
            )
        }
    }
}

@Composable
private fun DevContactChip(
    icon: ImageVector,
    label: String,
    value: String,
    textSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val iconSize = if (compact) 22.dp else 32.dp
    val padH = if (compact) 10.dp else 12.dp
    val padV = if (compact) 8.dp else 14.dp
    val gap = if (compact) 8.dp else 12.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.size(gap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = textSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 1.dp else 2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountSubPageHeader(
    title: String,
    titleSize: TextUnit,
    gap: Dp,
    onBack: () -> Unit,
    showBack: Boolean = true,
    backSize: Dp = 44.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (showBack) {
            YohoBackButton(onClick = onBack, size = backSize)
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegalLinkRow(
    label: String,
    textSize: TextUnit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = textSize,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            horizontalArrangement = Arrangement.spacedBy(metrics.blockGap),
        ) {
            if (metrics.isTablet) {
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
                    if (registerMode) R.string.account_sign_up
                    else R.string.account_sign_in,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
