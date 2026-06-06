package com.example.testapp

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun validEmail_returnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("student@test.com"))
    }

    @Test
    fun invalidEmail_returnsFalse() {
        assertFalse(ValidationUtils.isValidEmail("studenttestcom"))
    }

    @Test
    fun validPassword_returnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("123456"))
    }

    @Test
    fun shortPassword_returnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("123"))
    }

    @Test
    fun validRoomStatus_returnsTrue() {
        assertTrue(ValidationUtils.isValidRoomStatus("available"))
    }
    @Test
    fun validGroupName_returnsTrue() {
        assertTrue(ValidationUtils.isValidGroupName("Study Group"))
    }

    @Test
    fun emptyText_returnsFalse() {
        assertFalse(ValidationUtils.isNotEmpty("   "))
    }
}