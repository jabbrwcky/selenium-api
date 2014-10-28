package com.xing.qa.selenium.grid.hub;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * PrioritizerTest
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class PrioritizerTest {

    private Prioritizer sut = new Prioritizer();

    @Test
    public void testPriorityResolution() {
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("_CI", Boolean.TRUE);
        Map<String, Object> b = new HashMap<String, Object>();

        Assert.assertEquals(sut.compareTo(a, b), -1);
        Assert.assertEquals(sut.compareTo(b, a), 1);
        Assert.assertEquals(sut.compareTo(a, a), 0);
        Assert.assertEquals(sut.compareTo(b, b), 0);
    }

}
