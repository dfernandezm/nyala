package com.nyala.server.infrastructure.process;

import io.reactivex.Observable;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteArrayReactiveReader extends ProcessOutputReactiveReader<byte[]> {

    private static final int BUFFER_SIZE_BYTES = 64;

    public ByteArrayReactiveReader(List<String> command) {
        super(command);
    }

    @Override
    void readStream(InputStream inputStream) {
        byte[] buffer = new byte[BUFFER_SIZE_BYTES];
        try {
            while (!closingStream && IOUtils.read(inputStream, buffer) >= 0) {
             super.processInputs.onNext(buffer);
             buffer = new byte[BUFFER_SIZE_BYTES];
            }
            this.processInputs.onComplete();
        } catch (Throwable e) {
            System.out.println("Error reading");
            this.processInputs.onError(e);
        }
    }

    @Override
    void readErrorStream(InputStream errorStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(errorStream));
        String line;
        try {
            ArrayList<String> buffer = new ArrayList<>();
            while((line = br.readLine()) != null) {
                buffer.add(line);
            }
            if(buffer.size() > 0) {
                System.out.println("Error buffer");
                this.processInputs.onError(new Throwable(Arrays.toString(buffer.toArray())));
            }
        } catch (Throwable t) {
            System.out.println("Error stream t");
            this.processInputs.onError(t);
        }
    }

    @Override
    public Observable<byte[]> execute() {
        super.startAsync();
        return this.processInputs;
    }
}