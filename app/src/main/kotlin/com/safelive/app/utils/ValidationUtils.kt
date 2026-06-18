package com.safelive.app.utils

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace("[\\s\\-()]".toRegex(), "")
        return cleaned.matches(Regex("^[6-9]\\d{9}$"))
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    fun isPasswordMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isValidOtp(otp: String): Boolean {
        return otp.matches(Regex("^\\d{6}$"))
    }

    fun isValidTitle(title: String): Boolean {
        return title.trim().length in 5..200
    }

    fun isValidDescription(description: String): Boolean {
        return description.trim().length >= 20
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email is required")
            !isValidEmail(email) -> ValidationResult(false, "Invalid email address")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password is required")
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters")
            !password.any { it.isDigit() } -> ValidationResult(false, "Password must contain at least one digit")
            !password.any { it.isLetter() } -> ValidationResult(false, "Password must contain at least one letter")
            else -> ValidationResult(true)
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number is required")
            !isValidPhone(phone) -> ValidationResult(false, "Enter a valid 10-digit Indian mobile number")
            else -> ValidationResult(true)
        }
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name is required")
            name.trim().length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            else -> ValidationResult(true)
        }
    }
}
