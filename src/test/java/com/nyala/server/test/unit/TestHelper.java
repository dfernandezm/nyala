package com.nyala.server.test.unit;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestHelper {

    public  File readFile(String testFile) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        File file = new File(classLoader.getResource(testFile).getFile());
        assertThat(file.exists(), is(true));
        return file;
    }
}
