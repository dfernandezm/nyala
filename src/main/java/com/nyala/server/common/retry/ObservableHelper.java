package com.nyala.server.common.retry;

import com.nyala.server.common.logging.LoggingUtils;
import com.mongodb.MongoException;
import com.nyala.server.common.errorhandling.ApiErrorException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class ObservableHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservableHelper.class);
    private static final Integer OPERATION_TIMEOUT = 5000;
    private static final Integer MAX_RETRIES = 3;
    private static final Integer BASE_RETRY_INTERVAL_INTERVAL = 500;

    public ObservableHelper() {
    }

    public static <T> Single<T> retry(Supplier<Single<T>> singleSupplier) {
        return retryWithLinearBackoff(LoggingUtils.logTiming(singleSupplier.get(), "MongoDB"));
    }

    public static <T> Single<T> retry(Supplier<Single<T>> singleSupplier, String operationDescription) {
        return retryWithLinearBackoff(LoggingUtils.logTiming(singleSupplier.get(), operationDescription));
    }

    public static <T> Single<T> retryWithExponentialBackoff(Supplier<Single<T>> singleSupplier) {
        return retryWithExponentialBackoff(singleSupplier.get());
    }

    private static <T> Single<T> retryWithLinearBackoff(Single<T> sourceChain) {
        return sourceChain.timeout((long)OPERATION_TIMEOUT, TimeUnit.MILLISECONDS).onErrorResumeNext(ObservableHelper::wrapMongoTimeout).retryWhen((errors) -> {
            return errors.zipWith(Observable.range(1, MAX_RETRIES + 1), ObservableHelper::checkRetryable).flatMap((retryHolder) -> {
                return retryOrThrowError(retryHolder, errors, ObservableHelper.RetryStrategy.LINEAR_BACKOFF);
            });
        });
    }

    private static <T> Single<T> retryWithExponentialBackoff(Single<T> sourceChain) {
        return sourceChain.timeout((long)OPERATION_TIMEOUT, TimeUnit.MILLISECONDS).onErrorResumeNext(ObservableHelper::wrapError).retryWhen((errors) -> {
            return errors.zipWith(Observable.range(1, MAX_RETRIES + 1), ObservableHelper::checkRetryable).flatMap((retryHolder) -> {
                return retryOrThrowError(retryHolder, errors, ObservableHelper.RetryStrategy.EXPONENTIAL_BACKOFF);
            });
        });
    }

    private static <T> Single<T> wrapMongoTimeout(Throwable t) {
        if (t instanceof TimeoutException) {
            throw new MongoException("Timeout for operation communicating with MongoDB");
        } else if (t instanceof MongoException) {
            throw new MongoException(t.getMessage());
        } else {
            return Observable.<T>error(t).toSingle();
        }
    }

    private static <T> Single<T> wrapError(Throwable t) {
        LOGGER.error("Error occurred: " + t);
        return Observable.<T>error(t).toSingle();
    }

    private static ObservableHelper.RetryHolder checkRetryable(Throwable err, Integer attempt) {
        ObservableHelper.RetryHolder retryHolder = new ObservableHelper.RetryHolder();
        if (isRetryableError(err)) {
            retryHolder.setAttempt(attempt);
            retryHolder.setException(err);
        } else {
            retryHolder.setException(err);
        }

        return retryHolder;
    }

    private static Boolean isRetryableError(Throwable t) {
        return t instanceof MongoException || t instanceof TimeoutException || !(t instanceof ApiErrorException);
    }

    private static Observable<Long> retryOrThrowError(ObservableHelper.RetryHolder retryHolder, Observable<? extends Throwable> errors, ObservableHelper.RetryStrategy retryStrategy) {
        if (retryHolder.getException() != null && isRetryableError(retryHolder.getException())) {
            Integer attempt = retryHolder.getAttempt();
            if (attempt < MAX_RETRIES + 1) {
                LOGGER.info("Retrying -- " + attempt + " " + Thread.currentThread().getName());
                return retryStrategy == ObservableHelper.RetryStrategy.LINEAR_BACKOFF ? waitLinearBackoff(attempt) : waitExponentialBackoff(attempt);
            } else {
                LOGGER.info("Giving up after " + (attempt - 1) + " attempts " + Thread.currentThread().getName());
                return errors.flatMap(Observable::error);
            }
        } else {
            return errors.flatMap(Observable::error);
        }
    }

    private static Observable<Long> waitLinearBackoff(Integer attempt) {
        return Observable.timer((long)(attempt * BASE_RETRY_INTERVAL_INTERVAL), TimeUnit.MILLISECONDS);
    }

    private static Observable<Long> waitExponentialBackoff(Integer attempt) {
        return Observable.timer((long)Math.pow(4.0D, (double)attempt), TimeUnit.SECONDS);
    }

    @Data
    private static class RetryHolder {
        private Integer attempt;
        private Throwable exception;
    }

    private static enum RetryStrategy {
        LINEAR_BACKOFF,
        EXPONENTIAL_BACKOFF;

        private RetryStrategy() {
        }
    }
}
