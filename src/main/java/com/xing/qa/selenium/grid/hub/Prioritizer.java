package com.xing.qa.selenium.grid.hub;

import java.util.Map;

/**
 * This Prioritizer implementations assigns higher weight to jobs running on an CI system.
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class Prioritizer implements org.openqa.grid.internal.listeners.Prioritizer {

    @Override
    public int compareTo(Map<String, Object> a, Map<String, Object> b) {
        double scoreA = new PriorityScore(a).score();
        double scoreB = new PriorityScore(b).score();

        double diff = scoreA - scoreB;

        if (diff > 0)
            return -1;
        else if (diff < 0 )
            return 1;
        else return 0;
    }
}
