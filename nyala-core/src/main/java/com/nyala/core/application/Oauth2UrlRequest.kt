package com.nyala.core.application

data class Oauth2UrlRequest(val oauth2ClientId: String, val oauth2ClientSecret: String, val userId: String)