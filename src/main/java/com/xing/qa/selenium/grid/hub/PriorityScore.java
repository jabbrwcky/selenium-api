package com.xing.qa.selenium.grid.hub;

import java.util.Map;

/**
 * PriorityScore
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class PriorityScore {

    private final Map<String, Object> caps;

    public PriorityScore(Map<String, Object> capabilities) {
        this.caps = capabilities;
    }

    public double score() {
        double score = 0;

        score = addCIPriority(score);

        return score;
    }

    private double addCIPriority(double score) {
        if (caps.containsKey("ci")) {
            return score + 100;
        } else {
            return score;
        }
    }

}
