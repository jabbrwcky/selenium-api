package com.xing.qa.selenium.grid.hub;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class ConfigurableCapabilityMatcherTest {

    private ConfigurableCapabilityMatcher sut;

    private Map<String,Object> requested;
    private Map<String,Object> windowsNode = new HashMap<String, Object>();

    @BeforeClass
    public void createMatcher() {
        sut = new ConfigurableCapabilityMatcher();
        windowsNode.put(CapabilityType.PLATFORM, Platform.VISTA);
        windowsNode.put(CapabilityType.BROWSER_NAME, "firefox");
        windowsNode.put(CapabilityType.VERSION, "32.0");
    }

    @AfterClass
    public void cleanUp() {
        sut = null;
        windowsNode = null;
        requested = null;
    }

    @BeforeMethod
    public void setUp() {
        requested = new HashMap<String, Object>();
    }

    @Test
    public void testMatchesNoConstraints() throws Exception {
        Assert.assertEquals(sut.matches(windowsNode, requested), true);
    }

    @Test
    public void testMatchesCICapsOnly() throws Exception {
        requested.put("_CI", Boolean.TRUE);
        Assert.assertEquals(sut.matches(windowsNode, requested), true);
    }

    @Test
    public void testMatchesUncheckedCaps() throws Exception {
        requested.put(CapabilityType.HAS_TOUCHSCREEN,"true");
        Assert.assertEquals(sut.matches(windowsNode, requested), true);
    }

    @Test
    public void testMatchesRvmVersion() throws Exception {
        requested.put(CapabilityType.VERSION,">=30.0");
        Assert.assertEquals(sut.matches(windowsNode, requested), true);
    }
}
