package com.nyala.server.test.unit;

import java.io.File;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TestHelper {

    public File readFile(String testFile) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File file = new File(classLoader.getResource(testFile).getFile());
        assertThat(file.exists(), is(true));
        return file;
    }

    public String readFileToString(String filePath) {
        try {
            File file = readFile(filePath);
            return new String(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            fail("Error reading test file " + filePath);
            return null;
        }
    }
}
