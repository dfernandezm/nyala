package com.nyala.core.infrastructure.config

import com.nyala.core.application.handler.StatusEndpointHandler
import org.koin.dsl.module

class StatusModule {

    val statusModule = module {
        single { StatusEndpointHandler() }
    }
}