package com.nyala.core.infrastructure.di

import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication

object KoinDIFactory {

    private val koinApps = HashMap<String, KoinApplication>()

    fun startNewApp(name: String, koinAppDeclaration: KoinAppDeclaration): KoinApplication {
        synchronized(koinApps) {
           return koinApps.computeIfAbsent(name) {
                 koinApplication(koinAppDeclaration)
            }
        }
    }

    @JvmStatic
    fun get(name: String): KoinApplication = koinApps[name]
            ?: error("Isolated KoinApplication for has not been started or found with $name")

    @JvmStatic
    fun get(name: String, koinAppDeclaration: KoinAppDeclaration): KoinApplication {
        return koinApps[name] ?: startNewApp(name, koinAppDeclaration)
    }
}