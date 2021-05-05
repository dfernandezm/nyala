package com.nyala.server.infrastructure.mail.old

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