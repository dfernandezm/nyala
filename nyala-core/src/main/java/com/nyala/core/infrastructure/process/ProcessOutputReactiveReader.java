package com.nyala.core.infrastructure.process;

import io.reactivex.subjects.ReplaySubject;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class ProcessOutputReactiveReader<T> implements OutputReaderCommand {

    private final ProcessBuilder processBuilder;
    private Process process;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    protected final ReplaySubject<T> processInputs = ReplaySubject.create();
    protected volatile boolean closingStream;

    public ProcessOutputReactiveReader(List<String> command) {
        processBuilder = new ProcessBuilder(command);
    }

    abstract void readStream(InputStream inputStream);

    abstract void readErrorStream(InputStream inputStream);

    public void startAsync() {
        executorService.submit(this::startProcess);
    }

    protected void startSync() {
        startProcess();
    }

    public void cancel() {
        System.out.println("Terminating");
        closingStream = true;
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("Terminated tasks");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            process.destroyForcibly();
            System.out.println("Terminated process");
            processInputs.onComplete();
        }
    }

    private void startProcess() {
        try {
            process = processBuilder.start();
            readInputStream(process);
            readErrorStream(process);
            process.waitFor();
        } catch (Throwable t) {
            this.processInputs.onError(t);
        }
    }


    private void readInputStream(Process process) {
        executorService.submit(() -> readStream(process.getInputStream()));
    }

    private void readErrorStream(Process process) {
       executorService.submit(() -> readErrorStream(process.getErrorStream()));
    }
}