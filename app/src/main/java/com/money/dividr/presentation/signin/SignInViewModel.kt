package com.money.dividr.presentation.signin

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.money.dividr.model.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(): ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()


    fun onSignInResult(result: SignInResult) {
        _state.update {
            it.copy(
                isSignInSuccessful = result.data != null,
                signInError = result.error
            )
        }
    }

    fun resetState(){
        _state.update { SignInState() }
    }
}