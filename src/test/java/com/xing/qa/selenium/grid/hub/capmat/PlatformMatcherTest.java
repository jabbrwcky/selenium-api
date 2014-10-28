package com.xing.qa.selenium.grid.hub.capmat;

import org.openqa.selenium.Platform;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PlatformMatcherTest {

    private PlatformMatcher sut = new PlatformMatcher();

    @Test(dataProvider = "platforms")
    public void testMatches(Object input, Object platform, boolean match) throws Exception {
        Assert.assertEquals(sut.matches(input, platform), match);
    }

    @Test(dataProvider = "external")
    public void testExtractPlatform(Object in, Platform resolution) throws Exception {
        Assert.assertEquals(sut.extractPlatform(in), resolution);
    }

    @DataProvider
    public Object[][] external() {
        return new Object[][]{
                {"win7", Platform.VISTA},
                {"vista", Platform.VISTA},
                {"Vista", Platform.VISTA},
                {"win8_1", Platform.WIN8_1},
                {"XP", Platform.XP},
                {"Win7", Platform.VISTA},
                {"win7", Platform.VISTA},
                {"Windows Server 2012", Platform.WIN8},
                {"windows 8", Platform.WIN8},
                {"win8", Platform.WIN8},
                {"windows 8.1", Platform.WIN8_1},
                {"win8.1", Platform.WIN8_1},
                {null, null},
                {"w8", null},
                {Platform.ANDROID, Platform.ANDROID}
        };
    }

    @DataProvider
    public Object[][] platforms() {
        return new Object[][]{
                {"win7", Platform.VISTA, true},
                {"win7", "windows 7", true},
                {"vista", Platform.VISTA, true},
                {"darwin", Platform.MAC, true},
                {Platform.ANY, Platform.LINUX, true},
                {"linux", Platform.LINUX, true},
                {"linux", Platform.UNIX, false},
                {null, Platform.XP, true},
        };
    }
}
