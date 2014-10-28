package com.xing.qa.selenium.grid.hub.capmat;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExactMatcherTest {

    private ExactMatcher sut = new ExactMatcher();

    @Test
    public void testMatches() throws Exception {
        Object a = new Object();
        Object b = new Object();
        Assert.assertFalse(sut.matches(a,b));
        Assert.assertTrue(sut.matches(a,a));
    }

    @Test
    public void testMatchesStrings() throws Exception {
        String a = "a";
        String b = "b";
        Assert.assertFalse(sut.matches(a,b));
        Assert.assertTrue(sut.matches(a,a));
    }
}
