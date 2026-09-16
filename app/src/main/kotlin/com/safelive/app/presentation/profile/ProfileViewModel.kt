package com.safelive.app.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.User
import com.safelive.app.domain.repository.ProfileRepository
import com.safelive.app.domain.repository.PincodeRepository
import com.safelive.app.domain.usecase.auth.LogoutUseCase
import com.safelive.app.utils.ImageUtils
import com.safelive.app.utils.ValidationUtils
import com.safelive.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
    val pincodeLookupMessage: String? = null,
    val isCheckingPincode: Boolean = false,
    val isPincodeValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val pincodeRepository: PincodeRepository,
    private val logoutUseCase: LogoutUseCase,
    private val imageUtils: ImageUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeCachedProfile()
        loadProfileOnceIfNeeded()
    }

    private fun observeCachedProfile() {
        viewModelScope.launch {
            profileRepository.getCachedProfile().collect { cached ->
                if (cached != null) {
                    _uiState.update {
                        it.copy(
                            user = cached,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun loadProfileOnceIfNeeded() {
        viewModelScope.launch {
            if (profileRepository.getCachedProfile().firstOrNull() == null) {
                refreshProfileFromNetwork()
            }
        }
    }

    private suspend fun refreshProfileFromNetwork() {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = profileRepository.getProfile()) {
            is Resource.Success -> _uiState.update { it.copy(user = result.data, isLoading = false, error = null) }
            is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            is Resource.OtpRequired -> Unit
            Resource.Loading -> Unit
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var compressedFile: File? = null
            try {
                compressedFile = imageUtils.compressImage(uri)
                if (compressedFile == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to process image") }
                    return@launch
                }

                when (val result = profileRepository.uploadProfilePicture(compressedFile.absolutePath)) {
                    is Resource.Success -> _uiState.update { it.copy(user = result.data, isLoading = false) }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.OtpRequired -> Unit
                    Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            } finally {
                compressedFile?.delete()
            }
        }
    }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val pincodeRepository: PincodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var pincodeLookupJob: Job? = null

    init {
        viewModelScope.launch {
            profileRepository.getCachedProfile().firstOrNull()?.let { user ->
                initWith(user)
            }
        }
    }

    fun initWith(user: User?) {
        _uiState.update {
            it.copy(
                fullName = user?.fullName ?: "",
                phone = user?.phone ?: "",
                address = user?.address ?: "",
                pincode = user?.pincode ?: "",
                pincodeLookupMessage = null,
                isCheckingPincode = false,
                isPincodeValid = false
            )
        }

        val existingPincode = user?.pincode.orEmpty().trim()
        if (existingPincode.length == 6 && ValidationUtils.isValidPincode(existingPincode)) {
            lookupPincode(existingPincode)
        }
    }

    fun onFullNameChange(name: String) = _uiState.update { it.copy(fullName = name) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = ValidationUtils.digitsOnly(phone, 10)) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address) }
    fun onPincodeChange(pincode: String) {
        val trimmedPincode = ValidationUtils.digitsOnly(pincode, 6)
        _uiState.update {
            it.copy(
                pincode = trimmedPincode,
                pincodeLookupMessage = null,
                isCheckingPincode = false,
                isPincodeValid = false
            )
        }

        if (trimmedPincode.length == 6 && ValidationUtils.isValidPincode(trimmedPincode)) {
            lookupPincode(trimmedPincode)
        }
    }

    private fun lookupPincode(pincode: String) {
        pincodeLookupJob?.cancel()
        pincodeLookupJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPincode = true) }
            when (val result = pincodeRepository.lookupPincode(pincode)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isCheckingPincode = false,
                        pincodeLookupMessage = result.data,
                        isPincodeValid = true,
                        error = null
                    )
                }

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isCheckingPincode = false,
                        pincodeLookupMessage = result.message,
                        isPincodeValid = false
                    )
                }

                is Resource.OtpRequired -> Unit
                Resource.Loading -> Unit
            }
        }
    }

    fun updateProfile() {
        val current = _uiState.value
        when {
            !ValidationUtils.isValidName(current.fullName) -> {
                _uiState.update { it.copy(error = "Full name is required") }
                return
            }
            current.phone.isNotBlank() && !ValidationUtils.isValidPhone(current.phone) -> {
                _uiState.update { it.copy(error = "Enter a valid 10-digit mobile number") }
                return
            }
            current.pincode.isNotBlank() && !ValidationUtils.isValidPincode(current.pincode) -> {
                _uiState.update { it.copy(error = "Pincode must be a 6-digit number") }
                return
            }
            current.pincode.trim().length == 6 && !current.isPincodeValid -> {
                _uiState.update { it.copy(error = current.pincodeLookupMessage ?: "Please validate the pincode") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.updateProfile(
                fullName = current.fullName.trim(),
                mobile = current.phone.trim().ifBlank { null },
                address = current.address.trim().ifBlank { null },
                pincode = current.pincode.trim().ifBlank { null }
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true, error = null) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.OtpRequired -> Unit
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
