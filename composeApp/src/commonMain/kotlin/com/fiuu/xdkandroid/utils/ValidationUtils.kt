package com.fiuu.xdkandroid.utils


object ValidationUtils {

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && EMAIL_REGEX.matches(email)
    }

    fun isValidPhone(phone: String?) : Boolean {
        if (phone.isNullOrBlank()) return false
        val cleaned = phone.replace("\\s|-".toRegex(),"")
        return cleaned.matches("^\\+?[0-9]{7,15}$".toRegex())
    }
}