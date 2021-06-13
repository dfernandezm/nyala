package com.nyala.core.infrastructure.process;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReadProcessOutput implements OutputReaderCommand {

    private final ProcessBuilder processBuilder;
    private Process process;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final ReplaySubject<String> processInputs = ReplaySubject.create();

    public ReadProcessOutput(List<String> command) {
        processBuilder = new ProcessBuilder(command);
    }

    public void startAsync() {
        executorService.submit(() -> {
            try {
                process = processBuilder.start();
                readInputStream(process);
                readErrorStream(process);
                process.waitFor();
            } catch (Throwable t) {
                this.processInputs.onError(t);
            }
        });
    }

    public void cancel() {
        System.out.println("Terminating");
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

    public Observable<String> execute() {
        return processInputs;
    }

    private void readInputStream(Process process) {
        executorService.submit(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            try {
                while((line = br.readLine()) != null) this.processInputs.onNext(line);
                this.processInputs.onComplete();
            } catch (IOException e) {
                this.processInputs.onError(e);
            }
        });
    }

    private void readErrorStream(Process process) {
       executorService.submit(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            try {
                ArrayList<String> buffer = new ArrayList<>();
                while((line = br.readLine()) != null) {
                    buffer.add(line);
                }
                if(buffer.size() > 0) {
                    this.processInputs.onError(new Throwable(Arrays.toString(buffer.toArray())));
                }
            } catch (IOException e) {
               this.processInputs.onError(e);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        ReadProcessOutput process = new ReadProcessOutput(List.of("ping", "google.com"));
        process.startAsync();
        process.execute().map(el -> el.replace("bytes","")).subscribe(
                (input) -> System.out.println("Input: " + input),
                (error) -> {},
                () -> System.out.println("Completed"));

        Thread.sleep(10000);
        process.cancel();
    }
}