package com.safelive.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safelive.app.domain.model.User
import com.safelive.app.domain.repository.PincodeRepository
import com.safelive.app.domain.usecase.auth.*
import com.safelive.app.utils.Resource
import com.safelive.app.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val loginMode: String = "local",
    val loginWith: String = "email",
    val officialRole: String = "Department Login",
    val securityQuestion: String = "",
    val expectedSecurityAnswer: String = "",
    val userSecurityAnswer: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val userType: String? = null
)

// Generates a random simple math question and its answer
private fun generateSecurityQuestion(): Pair<String, String> {
    val questions = listOf(
        Pair("3 + 4 = ?", "7"),
        Pair("8 - 5 = ?", "3"),
        Pair("2 * 6 = ?", "12"),
        Pair("9 - 3 = ?", "6"),
        Pair("4 + 7 = ?", "11"),
        Pair("5 * 3 = ?", "15"),
        Pair("12 - 4 = ?", "8"),
        Pair("6 + 9 = ?", "15"),
        Pair("3 * 7 = ?", "21"),
        Pair("15 - 6 = ?", "9"),
        Pair("8 + 4 = ?", "12"),
        Pair("6 * 5 = ?", "30")
    )
    return questions.random()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        refreshSecurityQuestion()
    }

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = ValidationUtils.digitsOnly(phone, 10), error = null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onLoginModeChange(mode: String) = _uiState.update { it.copy(loginMode = mode) }
    fun onLoginWithChange(with: String) = _uiState.update { it.copy(loginWith = with) }
    fun onOfficialRoleChange(role: String) = _uiState.update { it.copy(officialRole = role) }
    fun onSecurityAnswerChange(answer: String) = _uiState.update { it.copy(userSecurityAnswer = answer, error = null) }

    fun refreshSecurityQuestion() {
        val (question, answer) = generateSecurityQuestion()
        _uiState.update {
            it.copy(
                securityQuestion = question,
                expectedSecurityAnswer = answer,
                userSecurityAnswer = "",
                error = null
            )
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.userSecurityAnswer.trim() != state.expectedSecurityAnswer) {
            _uiState.update { it.copy(error = "Incorrect security answer") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val loginIdentifier = if (state.loginMode == "local" && state.loginWith == "phone") state.phone else state.email
            val result = loginUseCase(loginIdentifier, state.password)
            when (result) {
                is Resource.Success<*> -> {
                    val user = result.data as? User
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, userType = user?.userType) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val pincode: String = "",
    val pincodeLookupMessage: String? = null,
    val isCheckingPincode: Boolean = false,
    val isPincodeValid: Boolean = false,
    val password: String = "",
    val confirmPassword: String = "",
    val userType: String = "local",
    val officialRole: String = "department",
    val workerSpecialization: String = "",
    val workerSpecializationExpanded: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val pincodeRepository: PincodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var pincodeLookupJob: Job? = null

    fun onFullNameChange(name: String) = _uiState.update { it.copy(fullName = name, error = null) }
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = ValidationUtils.digitsOnly(phone, 10), error = null) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address, error = null) }
    fun onPincodeChange(pincode: String) {
        _uiState.update {
            it.copy(
                pincode = pincode,
                error = null,
                pincodeLookupMessage = null,
                isCheckingPincode = false,
                isPincodeValid = false
            )
        }

        pincodeLookupJob?.cancel()
        val trimmedPincode = ValidationUtils.digitsOnly(pincode, 6)
        _uiState.update { it.copy(pincode = trimmedPincode) }
        if (trimmedPincode.length == 6 && ValidationUtils.isValidPincode(trimmedPincode)) {
            pincodeLookupJob = viewModelScope.launch {
                _uiState.update { it.copy(isCheckingPincode = true) }
                when (val result = pincodeRepository.lookupPincode(trimmedPincode)) {
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

                    Resource.Loading -> Unit
                }
            }
        }
    }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }
    fun onConfirmPasswordChange(confirm: String) = _uiState.update { it.copy(confirmPassword = confirm, error = null) }
    fun onUserTypeChange(type: String) = _uiState.update { it.copy(userType = type) }
    fun onOfficialRoleChange(role: String) = _uiState.update { it.copy(officialRole = role) }
    fun onWorkerSpecializationChange(spec: String) = _uiState.update { it.copy(workerSpecialization = spec, workerSpecializationExpanded = false, error = null) }
    fun onWorkerSpecializationExpandedChange(expanded: Boolean) = _uiState.update { it.copy(workerSpecializationExpanded = expanded) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun register() {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                !ValidationUtils.isValidPhone(state.phone) -> {
                    _uiState.update { it.copy(error = "Enter a valid 10-digit mobile number") }
                    return@launch
                }
                state.pincode.isNotBlank() && !ValidationUtils.isValidPincode(state.pincode) -> {
                    _uiState.update { it.copy(error = "Enter a valid 6-digit pincode") }
                    return@launch
                }
                state.pincode.trim().length == 6 && state.isCheckingPincode -> {
                    _uiState.update { it.copy(error = "Please wait for pincode validation") }
                    return@launch
                }
                state.pincode.trim().length == 6 && !state.isPincodeValid -> {
                    _uiState.update { it.copy(error = state.pincodeLookupMessage ?: "Please validate the pincode") }
                    return@launch
                }
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = registerUseCase(
                state.fullName, state.email, state.phone,
                state.password, state.confirmPassword, state.userType,
                state.address, state.pincode, state.officialRole, state.workerSpecialization
            )
            when (result) {
                is Resource.Success<*> -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun sendOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = forgotPasswordUseCase(_uiState.value.email)
            when (result) {
                is Resource.Success<*> -> _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, message = result.data as? String)
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}

data class OtpUiState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false
)

@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val verifyOtpUseCase: VerifyOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun onOtpChange(otp: String) = _uiState.update { it.copy(otp = otp, error = null) }

    fun verifyOtp(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = verifyOtpUseCase(email, _uiState.value.otp)
            when (result) {
                is Resource.Success<*> -> _uiState.update { it.copy(isLoading = false, isVerified = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onNewPasswordChange(password: String) = _uiState.update { it.copy(newPassword = password, error = null) }
    fun onConfirmPasswordChange(confirm: String) = _uiState.update { it.copy(confirmPassword = confirm, error = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun resetPassword(email: String, otp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            val result = resetPasswordUseCase(email, otp, state.newPassword, state.confirmPassword)
            when (result) {
                is Resource.Success<*> -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
