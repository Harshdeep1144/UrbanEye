package com.example.urbaneye.ui.screens.profile

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urbaneye.domain.model.User

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val view = LocalView.current
    val userState by viewModel.userState.collectAsState()

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            when (val state = userState) {
                is UserState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Black)
                }
                is UserState.LoggedOut -> {
                    AuthSection(
                        onLogin = viewModel::login,
                        onSignUp = viewModel::signUp
                    )
                }
                is UserState.LoggedIn -> {
                    LoggedInContent(
                        user = state.user,
                        onLogout = viewModel::logout,
                        onUpdateProfile = viewModel::updateUserProfile
                    )
                }
                is UserState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${state.message}", color = Color.Red)
                        Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthSection(
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isLogin) "Welcome back" else "Create account",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, focusedLabelColor = Color.Black)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, focusedLabelColor = Color.Black)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, focusedLabelColor = Color.Black)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isLogin) onLogin(email, password) else onSignUp(email, password, name)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text(if (isLogin) "Login" else "Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { isLogin = !isLogin },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login",
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LoggedInContent(
    user: User,
    onLogout: () -> Unit,
    onUpdateProfile: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(user.name) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, focusedLabelColor = Color.Black)
                        )
                        Row {
                            TextButton(onClick = { 
                                onUpdateProfile(editedName)
                                isEditing = false 
                            }) {
                                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { isEditing = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    } else {
                        Text(
                            text = if (user.name.isEmpty()) "User" else user.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "Edit Profile",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.clickable { isEditing = true }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF3F3F3))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = user.rating.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F3F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileOptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Help"
                )
                ProfileOptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Wallet"
                )
                ProfileOptionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Activity"
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        val settingsItems = listOf(
            SettingsItem("Settings", Icons.Default.Settings),
            SettingsItem("Messages", Icons.Default.Email),
            SettingsItem("Legal", Icons.Default.Info),
            SettingsItem("Log Out", Icons.Default.ExitToApp, isDestructive = true)
        )

        items(settingsItems) { item ->
            SettingsRow(item, onClick = {
                if (item.title == "Log Out") onLogout()
            })
        }
    }
}

@Composable
fun ProfileOptionCard(modifier: Modifier = Modifier, icon: ImageVector, label: String) {
    Surface(
        modifier = modifier.height(90.dp),
        color = Color(0xFFF3F3F3),
        shape = RoundedCornerShape(16.dp),
        onClick = { /* Navigate */ }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

data class SettingsItem(val title: String, val icon: ImageVector, val isDestructive: Boolean = false)

@Composable
fun SettingsRow(item: SettingsItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.isDestructive) Color.Red else Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                fontSize = 18.sp,
                color = if (item.isDestructive) Color.Red else Color.Black,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (!item.isDestructive) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}
