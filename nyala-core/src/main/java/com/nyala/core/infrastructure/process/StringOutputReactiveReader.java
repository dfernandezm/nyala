package com.nyala.core.infrastructure.process;

import io.reactivex.Observable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringOutputReactiveReader extends ProcessOutputReactiveReader<String> {

    public StringOutputReactiveReader(List<String> command) {
        super(command);
    }

    @Override
    void readStream(InputStream inputStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while((line = br.readLine()) != null) this.processInputs.onNext(line);
            this.processInputs.onComplete();
        } catch (IOException e) {
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
    public Observable<String> execute() {
        startAsync();
        return this.processInputs;
    }
}