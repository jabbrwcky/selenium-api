package com.xing.qa.selenium.grid.hub.capmat;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class RubyVersionMatcherTest {

    private RubyVersionMatcher sut = new RubyVersionMatcher();

    @Test(dataProvider = "versions")
    public void testMatches(String requested, String provided, boolean shouldMatch) throws Exception {
        Assert.assertEquals(sut.matches(requested, provided), shouldMatch);
    }

    @DataProvider
    public Object[][] versions() {
        return new Object[][]{
                {"1.0", "1.0", true},
                {"1.0", "1.0.1", false},
                {"1.0", "0.9", false},
                {"< 1.0", "1.0", false},
                {"< 1.0", "0.99", true},
                {"<1.0", "0.99", true},
                {"<= 1.0", "1.0", true},
                {"<= 1.0", "0.99", true},
                {"<=1.0", "0.99", true},
                {">= 1.0", "1.0", true},
                {">= 1.0", "1.1", true},
                {">=1.0", "1.1", true},
                {">= 1.0", "2.0", true},
                {"~> 0.2.0", "0.3.9", true},
                {"~> 0.2.0", "1.0", false},
                {"~> 1.2", "1.3", true},
                {"~>1.2", "1.3", true},
                {"~> 1.2", "1.1", false},
                {"~> 1.2", "2.2", false},
        };
    }
}
