package com.nyala.server.infrastructure.config

import com.nyala.server.application.handler.StatusEndpointHandler
import org.koin.dsl.module

class StatusModule {

    val statusModule = module {
        single { StatusEndpointHandler() }
    }
}