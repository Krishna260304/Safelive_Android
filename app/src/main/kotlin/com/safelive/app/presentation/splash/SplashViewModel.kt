package com.safelive.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object Login : SplashDestination()
    data object CitizenDashboard : SplashDestination()
    data object OfficialDashboard : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            delay(1800) 
            val isLoggedIn = authRepository.isLoggedIn().first()
            if (isLoggedIn) {
                val userType = authRepository.getUserType().first()
                _destination.value = if (userType?.lowercase() == "official") {
                    SplashDestination.OfficialDashboard
                } else {
                    SplashDestination.CitizenDashboard
                }
            } else {
                _destination.value = SplashDestination.Login
            }
        }
    }
}
