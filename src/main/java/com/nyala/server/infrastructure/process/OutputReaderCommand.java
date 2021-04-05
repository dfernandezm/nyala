package com.nyala.server.infrastructure.process;

import io.reactivex.Observable;

public interface OutputReaderCommand<T> {
    Observable<T> execute();
    void cancel();
}
