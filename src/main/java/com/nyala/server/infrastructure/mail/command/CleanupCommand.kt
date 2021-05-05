package com.nyala.server.infrastructure.mail.command

data class CleanupCommand(val user: String, val action: MailAction, val label: String, val timeframe: Timeframe)