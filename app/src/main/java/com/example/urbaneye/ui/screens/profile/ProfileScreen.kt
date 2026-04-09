package com.example.urbaneye.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urbaneye.domain.model.User
import com.example.urbaneye.ui.theme.LocalIsDarkTheme
import com.example.urbaneye.ui.theme.LocalThemeToggle
import com.example.urbaneye.ui.theme.UrbanEyeColors
import com.example.urbaneye.ui.utils.SetStatusBarColor

// ── Navigation State ──────────────────────────────────────────
enum class ProfileView {
    MAIN, REPORTS, TRUST, PREFS, NOTIFICATIONS, SECURITY, PRIVACY
}

// Data model for reports to prevent hardcoded dates
private data class ReportItem(val title: String, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val userState by viewModel.userState.collectAsState()
    var currentView by remember { mutableStateOf(ProfileView.MAIN) }

    SetStatusBarColor(
        backgroundColor = MaterialTheme.colorScheme.background,
        darkIcons = !com.example.urbaneye.ui.theme.LocalIsDarkTheme.current
    )

    BackHandler(enabled = currentView != ProfileView.MAIN) {
        currentView = ProfileView.MAIN
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (userState is UserState.LoggedIn) {
                val title = when (currentView) {
                    ProfileView.MAIN -> "PROFILE"
                    ProfileView.REPORTS -> "MY REPORTS"
                    ProfileView.TRUST -> "URBAN TRUST"
                    ProfileView.PREFS -> "PREFERENCES"
                    ProfileView.NOTIFICATIONS -> "NOTIFICATIONS"
                    ProfileView.SECURITY -> "SECURITY"
                    ProfileView.PRIVACY -> "PRIVACY POLICY"
                }
                CenterAlignedTopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        if (currentView != ProfileView.MAIN) {
                            IconButton(onClick = { currentView = ProfileView.MAIN }) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = userState) {
                is UserState.Loading -> ProfileSkeleton()
                is UserState.LoggedOut -> AuthSection(viewModel::login, viewModel::signUp)
                is UserState.LoggedIn -> {
                    Crossfade(targetState = currentView, label = "view_transition") { view ->
                        when (view) {
                            ProfileView.MAIN -> ProfileContent(state.user, onLogout = viewModel::logout, onUpdate = viewModel::updateUserProfile, navigate = { currentView = it })
                            ProfileView.REPORTS -> ReportsView()
                            ProfileView.TRUST -> TrustView()
                            ProfileView.PREFS -> PrefsView()
                            ProfileView.NOTIFICATIONS -> NotificationsView()
                            ProfileView.SECURITY -> SecurityView()
                            ProfileView.PRIVACY -> PrivacyView()
                        }
                    }
                }
                is UserState.Error -> ErrorState(state.message) { viewModel.logout() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth Section (Login / Sign Up)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthSection(onLogin: (String, String) -> Unit, onSignUp: (String, String, String) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- App Branding ---
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(UrbanEyeColors.HoloPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Visibility, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (isLoginMode) "WELCOME BACK" else "JOIN URBANEYE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            if (isLoginMode) "Sign in to monitor your city" else "Create an account to start reporting",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // --- Form Fields ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedVisibility(visible = !isLoginMode) {
                AuthTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    icon = Icons.Rounded.Person
                )
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                icon = Icons.Rounded.Email,
                keyboardType = KeyboardType.Email
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Rounded.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )
        }

        Spacer(Modifier.height(32.dp))

        // --- Action Button ---
        Button(
            onClick = {
                // FIXED: Adjusted onSignUp parameter order to match ViewModel: (email, pass, name)
                if (isLoginMode) onLogin(email, password)
                else onSignUp(email, password, name)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UrbanEyeColors.HoloTeal),
        ) {
            Text(
                if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = UrbanEyeColors.BankBlack
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Toggle Mode ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isLoginMode) "Don't have an account?" else "Already a citizen?",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                    if (isLoginMode) "Sign Up" else "Login",
                    color = UrbanEyeColors.HoloPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(icon, null, tint = UrbanEyeColors.HoloPurple.copy(0.7f)) },
        trailingIcon = {
            if (isPassword && onPasswordToggle != null) {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null,
                        tint = Color.Gray
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UrbanEyeColors.HoloTeal,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-Views
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportsView() {
    val sampleReports = remember {
        listOf(
            ReportItem("Pothole on 5th Ave", "Oct 20, 2024"),
            ReportItem("Broken streetlight - Sector 4", "Oct 18, 2024"),
            ReportItem("Waste overflow - Central Park", "Oct 15, 2024"),
            ReportItem("Graffiti on Heritage Wall", "Oct 12, 2024")
        )
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(sampleReports) { report ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(UrbanEyeColors.Lavender.copy(0.4f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.History, null, tint = UrbanEyeColors.PureWhite, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(report.title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        Text(report.date, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustView() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(UrbanEyeColors.Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Verified, null, tint = UrbanEyeColors.BankBlack, modifier = Modifier.size(50.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Verified Contributor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(32.dp))
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text("Trust Level: Elite", color = UrbanEyeColors.RoyalEmerald, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = 0.85f, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = UrbanEyeColors.RoyalEmerald, trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f))
            }
        }
    }
}

@Composable
private fun PrefsView() {
    val darkMode = LocalIsDarkTheme.current
    val toggleTheme = LocalThemeToggle.current
    Column(Modifier.padding(20.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Switch(
                    checked = darkMode,
                    onCheckedChange = toggleTheme,
                    colors = SwitchDefaults.colors(checkedThumbColor = UrbanEyeColors.HoloTeal)
                )
            }
        }
    }
}

@Composable
private fun NotificationsView() {
    var reportUpdates by remember { mutableStateOf(true) }
    var communityAlerts by remember { mutableStateOf(true) }
    var promotional by remember { mutableStateOf(false) }

    Column(Modifier.padding(20.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
            Column {
                NotificationRow("Report Updates", reportUpdates) { reportUpdates = it }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onBackground.copy(0.05f))
                NotificationRow("Community Alerts", communityAlerts) { communityAlerts = it }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onBackground.copy(0.05f))
                NotificationRow("Promotional", promotional) { promotional = it }
            }
        }
    }
}

@Composable
private fun NotificationRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = UrbanEyeColors.HoloTeal,
                checkedTrackColor = UrbanEyeColors.HoloTeal.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SecurityView() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Security, null, tint = UrbanEyeColors.HoloPurple, modifier = Modifier.size(48.dp))
                Text("Your data is encrypted", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(UrbanEyeColors.SoftBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Change Password", color = UrbanEyeColors.BankBlack)
        }
    }
}

@Composable
private fun PrivacyView() {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Our Commitment to Privacy", style = MaterialTheme.typography.titleLarge, color = UrbanEyeColors.SkyBlue, fontWeight = FontWeight.Bold)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "UrbanEye primarily operates as a community-driven infrastructure monitoring tool.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("• Detection: Authorized users identify potholes and road hazards.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text("• Visualization: Verified data is processed and shared.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Content & Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileContent(user: User, onLogout: () -> Unit, onUpdate: (String) -> Unit, navigate: (ProfileView) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(user.name) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(UrbanEyeColors.HoloPurple)
                            .padding(4.dp)
                    ) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                            Text((user.name.firstOrNull() ?: 'U').uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnimatedContent(targetState = isEditing, label = "edit_anim") { editing ->
                    if (editing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UrbanEyeColors.HoloPurple,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onUpdate(editedName); isEditing = false }) {
                                    Text("SAVE", color = UrbanEyeColors.HoloTeal, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { isEditing = false; editedName = user.name }) {
                                    Text("CANCEL", color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(user.name.ifEmpty { "Urban Citizen" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Edit, null, tint = UrbanEyeColors.HoloPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("URBAN SCORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                        Text(user.rating.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = UrbanEyeColors.HoloTeal)
                    }
                    Icon(Icons.Rounded.TrendingUp, null, tint = UrbanEyeColors.HoloTeal, modifier = Modifier.size(32.dp))
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionItem(Modifier.weight(1f), Icons.Rounded.Article, "Reports", UrbanEyeColors.PaleViolet) { navigate(ProfileView.REPORTS) }
                QuickActionItem(Modifier.weight(1f), Icons.Rounded.VerifiedUser, "Trust", UrbanEyeColors.Mint) { navigate(ProfileView.TRUST) }
                QuickActionItem(Modifier.weight(1f), Icons.Rounded.Settings, "Prefs", UrbanEyeColors.SoftBlue) { navigate(ProfileView.PREFS) }
            }
        }

        item {
            Column {
                Text("ACCOUNT SETTINGS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsRow(Icons.Rounded.Notifications, "Notifications") { navigate(ProfileView.NOTIFICATIONS) }
                        SettingsRow(Icons.Rounded.Security, "Security") { navigate(ProfileView.SECURITY) }
                        SettingsRow(Icons.Rounded.Description, "Privacy Policy") { navigate(ProfileView.PRIVACY) }
                        SettingsRow(Icons.Rounded.Logout, "Sign Out", isDestructive = true, showDivider = false, onClick = onLogout)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun QuickActionItem(modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, isDestructive: Boolean = false, showDivider: Boolean = true, onClick: () -> Unit) {
    val color = if (isDestructive) UrbanEyeColors.SunsetCrimson else MaterialTheme.colorScheme.onSurface
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color.copy(0.7f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(18.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (!isDestructive) Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        if (showDivider) HorizontalDivider(Modifier.padding(horizontal = 22.dp), color = MaterialTheme.colorScheme.onBackground.copy(0.05f))
    }
}

@Composable
private fun ProfileSkeleton() {
    val alphaAnim = rememberInfiniteTransition(label = "skeleton")
    val alpha by alphaAnim.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "skeleton_alpha"
    )

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val skeletonColor = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha * 0.15f)
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(110.dp).clip(CircleShape).background(skeletonColor))
        Spacer(Modifier.height(20.dp))
        Box(Modifier.width(150.dp).height(28.dp).clip(RoundedCornerShape(8.dp)).background(skeletonColor))
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(skeletonColor))
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) { Box(Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(20.dp)).background(skeletonColor)) }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = UrbanEyeColors.SunsetCrimson, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = UrbanEyeColors.SunsetCrimson, textAlign = TextAlign.Center)
            TextButton(onClick = onRetry) { Text("RETRY", color = UrbanEyeColors.HoloTeal) }
        }
    }
}