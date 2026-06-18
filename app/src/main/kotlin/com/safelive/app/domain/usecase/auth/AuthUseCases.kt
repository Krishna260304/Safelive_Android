package com.safelive.app.domain.usecase.auth

import com.safelive.app.domain.repository.AuthRepository
import com.safelive.app.utils.ValidationUtils
import com.safelive.app.utils.Resource
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<*> {
        val trimmedIdentifier = email.trim()
        val identifierValidation = if (trimmedIdentifier.contains("@")) {
            ValidationUtils.validateEmail(trimmedIdentifier)
        } else {
            ValidationUtils.validatePhone(trimmedIdentifier)
        }
        if (!identifierValidation.isValid) return Resource.Error(identifierValidation.errorMessage ?: "Invalid login identifier")

        if (password.isBlank()) return Resource.Error("Password is required")

        return authRepository.login(trimmedIdentifier, password)
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
        pincode: String
    ): Resource<*> {
        val nameValidation = ValidationUtils.validateName(fullName)
        if (!nameValidation.isValid) return Resource.Error(nameValidation.errorMessage ?: "Invalid name")

        val emailValidation = ValidationUtils.validateEmail(email)
        if (!emailValidation.isValid) return Resource.Error(emailValidation.errorMessage ?: "Invalid email")

        val phoneValidation = ValidationUtils.validatePhone(phone)
        if (!phoneValidation.isValid) return Resource.Error(phoneValidation.errorMessage ?: "Invalid phone")

        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.isValid) return Resource.Error(passwordValidation.errorMessage ?: "Invalid password")

        if (!ValidationUtils.isPasswordMatch(password, confirmPassword)) {
            return Resource.Error("Passwords do not match")
        }

        if (userType.isBlank()) return Resource.Error("Please select user type")

        return authRepository.register(fullName.trim(), email.trim(), phone.trim(), password, userType, address.trim(), pincode.trim())
    }
}

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Resource<*> {
        val emailValidation = ValidationUtils.validateEmail(email)
        if (!emailValidation.isValid) return Resource.Error(emailValidation.errorMessage ?: "Invalid email")
        return authRepository.forgotPassword(email.trim())
    }
}

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String): Resource<*> {
        if (!ValidationUtils.isValidOtp(otp)) return Resource.Error("Enter a valid 6-digit OTP")
        return authRepository.verifyOtp(email, otp)
    }
}

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String, newPassword: String, confirmPassword: String): Resource<*> {
        val passwordValidation = ValidationUtils.validatePassword(newPassword)
        if (!passwordValidation.isValid) return Resource.Error(passwordValidation.errorMessage ?: "Invalid password")
        if (!ValidationUtils.isPasswordMatch(newPassword, confirmPassword)) {
            return Resource.Error("Passwords do not match")
        }
        return authRepository.resetPassword(email, otp, newPassword)
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
