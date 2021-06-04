package com.nyala.server.infrastructure.di

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

interface IsolatedKoinComponent: KoinComponent {
    override fun getKoin(): Koin = KoinDI.get().koin
}