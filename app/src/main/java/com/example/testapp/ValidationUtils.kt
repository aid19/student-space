package com.example.testapp

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        val trimmedEmail = email.trim()
        return trimmedEmail.contains("@") && trimmedEmail.contains(".")
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isNotEmpty(text: String): Boolean {
        return text.trim().isNotEmpty()
    }

    fun isValidGroupName(groupName: String): Boolean {
        return groupName.trim().length >= 3
    }

    fun isValidRoomStatus(status: String): Boolean {
        return status == "available" || status == "occupied"
    }
}