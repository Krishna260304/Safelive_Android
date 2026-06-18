package com.safelive.app.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class UserDtoTest {

    @Test
    fun `toDomain uses empty string when fullName is missing`() {
        val dto = UserDto(
            id = "1",
            email = "user@example.com",
            phone = null,
            fullName = null,
            userType = "citizen",
            officialRole = null,
            workerSpecialization = null,
            address = null,
            pincode = null,
            createdAt = null,
            isVerified = false
        )

        val domain = dto.toDomain()

        assertEquals("", domain.fullName)
    }

    @Test
    fun `toDomain preserves provided fullName`() {
        val dto = UserDto(
            id = "2",
            email = "official@example.com",
            phone = "+1234567890",
            fullName = "Alice Smith",
            userType = "official",
            officialRole = "Inspector",
            workerSpecialization = null,
            address = "Main Street",
            pincode = "123456",
            createdAt = "2026-06-17T00:00:00Z",
            isVerified = true
        )

        val domain = dto.toDomain()

        assertEquals("Alice Smith", domain.fullName)
        assertEquals("official", domain.userType)
    }
}