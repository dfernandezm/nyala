package com.nyala.server.infrastructure.process;

import lombok.Builder;

@Builder
public class OutputChunk<T> {
    private final T data;
    private final int sizeToRead;
}
