//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.fazula.server.common.routing;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.eventbus.MessageConsumer;
import lombok.Generated;

import java.beans.ConstructorProperties;
import java.util.function.Consumer;

public class EventBusDsl {
    private String address;
    private Consumer<Message<JsonObject>> handler;

    public void setUpConsumer(Vertx vertx) {
        MessageConsumer var10000 = vertx.eventBus().consumer(this.address);
        Consumer var10001 = this.handler;
        var10000.handler(var10001::accept);
    }

    @ConstructorProperties({"address", "handler"})
    @Generated
    EventBusDsl(String address, Consumer<Message<JsonObject>> handler) {
        this.address = address;
        this.handler = handler;
    }

    @Generated
    public static EventBusDsl.EventBusDslBuilder builder() {
        return new EventBusDsl.EventBusDslBuilder();
    }

    @Generated
    public String getAddress() {
        return this.address;
    }

    @Generated
    public Consumer<Message<JsonObject>> getHandler() {
        return this.handler;
    }

    @Generated
    public static class EventBusDslBuilder {
        @Generated
        private String address;
        @Generated
        private Consumer<Message<JsonObject>> handler;

        @Generated
        EventBusDslBuilder() {
        }

        @Generated
        public EventBusDsl.EventBusDslBuilder address(String address) {
            this.address = address;
            return this;
        }

        @Generated
        public EventBusDsl.EventBusDslBuilder handler(Consumer<Message<JsonObject>> handler) {
            this.handler = handler;
            return this;
        }

        @Generated
        public EventBusDsl build() {
            return new EventBusDsl(this.address, this.handler);
        }

        @Generated
        public String toString() {
            return "EventBusDsl.EventBusDslBuilder(address=" + this.address + ", handler=" + this.handler + ")";
        }
    }
}
