package com.bpkpad.peminjaman.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bpkpad.peminjaman.core.theme.*
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole

/**
 * [LOCAL] LoginScreen
 * Ownership: Auth Module
 * RBAC: None (pre-auth)
 * v1.0 2026-05-24
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.loginSuccess) {
        uiState.loginSuccess?.let { role ->
            viewModel.consumeLoginSuccess()
            onLoginSuccess(role)
        }
    }
    LoginContent(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF207125), Color(0xFF0D631B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(88.dp))

            // App Icon
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FolderOpen, "Logo", Modifier.size(50.dp), tint = Color.White)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("BPKPAD Balangan", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Sistem Peminjaman Dokumen", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.height(44.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Masuk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF207125))
                    Text("Masukkan kredensial Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        isError = uiState.error != null && uiState.username.isBlank()
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    if (passwordVisible) "Sembunyikan" else "Tampilkan"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onLogin() }),
                        isError = uiState.error != null && uiState.password.isBlank()
                    )

                    AnimatedVisibility(visible = uiState.error != null) {
                        uiState.error?.let { err ->
                            Spacer(Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = onLogin,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF207125))
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Login, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Masuk", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Gunakan akun dan password yang valid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "BPKPAD Kabupaten Balangan\nSistem Manajemen Peminjaman Dokumen v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, name = "Login Screen")
@Composable
private fun LoginScreen_Preview() {
    BpkpadTheme {
        LoginContent(LoginUiState(username = "budi"), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Login Error State")
@Composable
private fun LoginScreen_Error_Preview() {
    BpkpadTheme {
        LoginContent(LoginUiState(error = "Username atau password salah"), {}, {}, {})
    }
}
