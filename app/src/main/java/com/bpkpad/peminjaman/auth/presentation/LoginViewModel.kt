package com.bpkpad.peminjaman.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpkpad.peminjaman.auth.domain.usecase.LoginUseCase
import com.bpkpad.peminjaman.auth.domain.usecase.LogoutUseCase
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: UserRole? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(v: String) { _uiState.update { it.copy(username = v, error = null) } }
    fun onPasswordChange(v: String) { _uiState.update { it.copy(password = v, error = null) } }

    fun login() {
        val s = _uiState.value
        if (s.username.isBlank()) { _uiState.update { it.copy(error = "Username tidak boleh kosong") }; return }
        if (s.password.isBlank()) { _uiState.update { it.copy(error = "Password tidak boleh kosong") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = loginUseCase(s.username, s.password)) {
                is ResultState.Success -> _uiState.update { it.copy(isLoading = false, loginSuccess = r.data.role) }
                is ResultState.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }

    fun consumeLoginSuccess() { _uiState.update { it.copy(loginSuccess = null) } }
    fun logout() { viewModelScope.launch { logoutUseCase() } }
}
