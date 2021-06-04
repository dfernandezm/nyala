package com.nyala.server.infrastructure.di

import io.vertx.rxjava.core.AbstractVerticle

abstract class KoinVerticle: AbstractVerticle(), IsolatedKoinComponent { }
