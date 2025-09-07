package com.money.dividr.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileScreenState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileScreenState(isLoading = true))
    val uiState: StateFlow<ProfileScreenState> = _uiState.asStateFlow()

    private val _signOutComplete = MutableStateFlow(false)
    val signOutComplete: StateFlow<Boolean> = _signOutComplete.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.value = ProfileScreenState(user = auth.currentUser, isLoading = false)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _signOutComplete.value = true
            } catch (e: Exception) {
                 
                _uiState.value = _uiState.value.copy(error = "Sign out failed: ${e.message}")
            }
        }
    }

    fun resetSignOutComplete() {
        _signOutComplete.value = false
    }
}