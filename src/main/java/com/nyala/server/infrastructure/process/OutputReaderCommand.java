package com.nyala.server.infrastructure.process;

import io.reactivex.Observable;

public interface OutputReaderCommand {
    Observable<?> execute();
    void cancel();
}
