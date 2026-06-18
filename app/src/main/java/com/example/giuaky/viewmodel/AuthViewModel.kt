package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthRoleState {
    object Idle : AuthRoleState()
    object Loading : AuthRoleState()
    data class Admin(val uid: String) : AuthRoleState()
    data class User(val uid: String) : AuthRoleState()
    data class Error(val message: String) : AuthRoleState()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userRole: String = "user",
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _roleState = MutableStateFlow<AuthRoleState>(AuthRoleState.Idle)
    val roleState: StateFlow<AuthRoleState> = _roleState.asStateFlow()

    fun fetchUserRole() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _roleState.value = AuthRoleState.Error("Chưa đăng nhập")
            return
        }

        viewModelScope.launch {
            _roleState.value = AuthRoleState.Loading
            try {
                // Sử dụng repository để lấy role từ Realtime Database
                val role = repository.getUserRoleFromRealtimeDB(currentUser.uid)
                when (role) {
                    "admin" -> _roleState.value = AuthRoleState.Admin(currentUser.uid)
                    "user" -> _roleState.value = AuthRoleState.User(currentUser.uid)
                    else -> _roleState.value = AuthRoleState.User(currentUser.uid) // Default là user
                }
            } catch (e: Exception) {
                _roleState.value = AuthRoleState.Error(e.message ?: "Lỗi khi lấy quyền người dùng")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, userRole = user.role) }
                    fetchUserRole() // Sau khi login thành công thì fetch role ngay
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Đăng nhập thất bại") }
                }
            )
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.register(email, password, displayName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Đăng ký thất bại") }
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _roleState.value = AuthRoleState.Idle
        clearState()
    }

    fun clearState() = _uiState.update { AuthUiState() }
}
