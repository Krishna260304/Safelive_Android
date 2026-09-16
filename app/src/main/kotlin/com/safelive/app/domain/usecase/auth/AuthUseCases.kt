package com.safelive.app.domain.usecase.auth

import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.utils.ValidationUtils
import com.safelive.app.utils.Resource
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        identifier: String,
        password: String,
        expectedUserType: String? = null,
        expectedOfficialRole: String? = null
    ): Resource<*> {
        val trimmedIdentifier = identifier.trim()
        val identifierValidation = if (trimmedIdentifier.contains("@")) {
            ValidationUtils.validateEmail(trimmedIdentifier)
        } else {
            ValidationUtils.validatePhone(trimmedIdentifier)
        }
        if (!identifierValidation.isValid) return Resource.Error(identifierValidation.errorMessage ?: "Invalid login identifier")

        if (password.isBlank()) return Resource.Error("Password is required")

        return authRepository.login(
            identifier = trimmedIdentifier,
            password = password,
            expectedUserType = expectedUserType,
            expectedOfficialRole = expectedOfficialRole
        )
    }
}

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        userType: String,
        address: String,
        pincode: String,
        officialRole: String? = null,
        workerSpecialization: String? = null
    ): Resource<*> {
        val nameValidation = ValidationUtils.validateName(fullName)
        if (!nameValidation.isValid) return Resource.Error(nameValidation.errorMessage ?: "Invalid name")

        if (!(userType == "official" && officialRole == "worker" && email.isBlank())) {
            val emailValidation = ValidationUtils.validateEmail(email)
            if (!emailValidation.isValid) return Resource.Error(emailValidation.errorMessage ?: "Invalid email")
        }

        val phoneValidation = ValidationUtils.validatePhone(phone)
        if (!phoneValidation.isValid) return Resource.Error(phoneValidation.errorMessage ?: "Invalid phone")

        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.isValid) return Resource.Error(passwordValidation.errorMessage ?: "Invalid password")

        if (!ValidationUtils.isPasswordMatch(password, confirmPassword)) {
            return Resource.Error("Passwords do not match")
        }

        if (userType.isBlank()) return Resource.Error("Please select user type")
        
        if (userType == "official" && officialRole == "worker" && workerSpecialization.isNullOrBlank()) {
            return Resource.Error("Please select worker category")
        }

        val apiUserType = if (userType.equals("local", ignoreCase = true)) "citizen" else userType
        return authRepository.register(
            fullName.trim(), email.trim(), phone.trim(), password, apiUserType, address.trim(), pincode.trim(),
            if (userType == "official") officialRole else null,
            if (userType == "official" && officialRole == "worker") workerSpecialization else null
        )
    }
}

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, phone: String? = null): Resource<*> {
        if (email.isBlank() && phone.isNullOrBlank()) return Resource.Error("Email or phone is required")
        if (email.isNotBlank()) {
            val emailValidation = ValidationUtils.validateEmail(email)
            if (!emailValidation.isValid) return Resource.Error(emailValidation.errorMessage ?: "Invalid email")
        } else if (!ValidationUtils.isValidPhone(phone.orEmpty())) {
            return Resource.Error("Enter a valid 10-digit mobile number")
        }
        return authRepository.forgotPassword(email.trim().takeIf { it.isNotBlank() }, phone?.trim())
    }
}

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(challengeId: String, otp: String): Resource<*> {
        if (!ValidationUtils.isValidOtp(otp)) return Resource.Error("Enter a valid 6-digit OTP")
        return authRepository.verifyOtp(challengeId, otp)
    }
}

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String, newPassword: String, confirmPassword: String): Resource<*> {
        val passwordValidation = ValidationUtils.validatePassword(newPassword)
        if (!passwordValidation.isValid) return Resource.Error(passwordValidation.errorMessage ?: "Invalid password")
        if (!ValidationUtils.isPasswordMatch(newPassword, confirmPassword)) {
            return Resource.Error("Passwords do not match")
        }
        return authRepository.resetPassword(token, newPassword)
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> = authRepository.logout()
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.getMe()
}
