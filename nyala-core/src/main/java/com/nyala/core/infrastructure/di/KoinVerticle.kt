package com.nyala.core.infrastructure.di

import io.vertx.rxjava.core.AbstractVerticle

abstract class KoinVerticle: AbstractVerticle(), IsolatedKoinComponent { }
