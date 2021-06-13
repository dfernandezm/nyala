package com.nyala.core.infrastructure.mail.old

class EmailUtil {
    companion object {
        fun parseAddress(string: String): String {
            return if (string.contains("<")) {
                string.substringAfter("<").substringBefore(">")
            } else {
                string
            }
        }
    }
}