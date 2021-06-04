package com.nyala.server.infrastructure.di

import io.vertx.rxjava.core.AbstractVerticle
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

abstract class IsolatedKoinVerticle: AbstractVerticle(), KoinComponent {
    abstract fun getAppName(): String
    override fun getKoin(): Koin = KoinDIFactory.get(getAppName()).koin
}