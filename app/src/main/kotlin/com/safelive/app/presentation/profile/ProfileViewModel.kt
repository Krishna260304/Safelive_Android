package com.safelive.app.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.User
import com.safelive.app.domain.repository.ProfileRepository
import com.safelive.app.domain.usecase.auth.LogoutUseCase
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val loggedOut: Boolean = false
)

data class EditProfileUiState(
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val pincode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.getProfile()) {
                is Resource.Success -> _uiState.update { it.copy(user = result.data, isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                Resource.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun uploadPhoto(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("profile_pic", ".jpg", context.cacheDir)
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                when (val result = profileRepository.uploadProfilePicture(tempFile.absolutePath)) {
                    is Resource.Success -> _uiState.update { it.copy(user = result.data, isLoading = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun initWith(user: User?) {
        _uiState.update {
            it.copy(
                fullName = user?.fullName ?: "",
                phone = user?.phone ?: "",
                address = user?.address ?: "",
                pincode = user?.pincode ?: ""
            )
        }
    }

    fun onFullNameChange(name: String) = _uiState.update { it.copy(fullName = name) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = phone) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address) }
    fun onPincodeChange(pincode: String) = _uiState.update { it.copy(pincode = pincode) }

    fun updateProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.updateProfile(
                fullName = _uiState.value.fullName,
                mobile = _uiState.value.phone,
                address = _uiState.value.address,
                pincode = _uiState.value.pincode
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun uploadPhoto(path: String) {
        viewModelScope.launch {
            profileRepository.uploadProfilePicture(path)
        }
    }
}
