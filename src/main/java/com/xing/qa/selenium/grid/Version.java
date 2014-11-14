package com.xing.qa.selenium.grid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Version
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class Version {

    private static String version = "unknown";

    static{
        try {
            version = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("version"))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("Selenium API version %s", version));
    }

    public static String version() {
        return version;
    }

}
