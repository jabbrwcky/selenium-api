package com.xing.qa.selenium.grid.hub;

import java.util.Map;

/**
 * This Prioritizer implementations assigns higher weight to jobs running on an CI system.
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class Prioritizer implements org.openqa.grid.internal.listeners.Prioritizer {

    private static final String CI = "ci";

    @Override
    public int compareTo(Map<String, Object> a, Map<String, Object> b) {
        boolean ciA = a.containsKey(CI);
        boolean ciB = b.containsKey(CI);

        if (ciA && !ciB) {
            return -1;
        } else if ( ciB && !ciA) {
            return 1;
        } else return 0;
    }
}
