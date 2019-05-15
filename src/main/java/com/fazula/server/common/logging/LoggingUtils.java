package com.fazula.server.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;
import rx.schedulers.TimeInterval;

public class LoggingUtils {
    private static Logger LOGGER = LoggerFactory.getLogger(LoggingUtils.class);

    public LoggingUtils() {
    }

    public static <T> Single<T> logTiming(Single<T> singleChain, String operationDescription) {
        return singleChain.toObservable().timeInterval().doOnNext((event) -> {
            LOGGER.info(operationDescription + " call took {} ms", event.getIntervalInMilliseconds());
        }).map(TimeInterval::getValue).toSingle();
    }
}
