package com.nyala.server.infrastructure.process;

import java.util.List;

public class MainReader {
    public static void main(String[] args) throws InterruptedException {
        byteArrayReader();
    }

    private static void byteArrayReader() throws InterruptedException {
        OutputReaderCommand<byte[]> outputCmd = new ByteArrayReactiveReader(List.of("ping", "google.com"));
        outputCmd.execute().subscribe(
                (input) -> System.out.println("Input: " + input),
                (error) -> System.out.println("Error: " + error),
                () -> System.out.println("Completed")
        );

        Thread.sleep(10000);
        outputCmd.cancel();
    }

    private static void stringReader() throws InterruptedException {
        OutputReaderCommand<String> outputCmd = new StringOutputReactiveReader(List.of("ping", "google.com"));
        outputCmd.execute().subscribe(
                (input) -> System.out.println("Input: " + input),
                (error) -> System.out.println("Error: " + error),
                () -> System.out.println("Completed")
        );

        Thread.sleep(10000);
        outputCmd.cancel();
    }
}
