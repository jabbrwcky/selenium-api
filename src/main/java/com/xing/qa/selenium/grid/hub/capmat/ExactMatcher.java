package com.xing.qa.selenium.grid.hub.capmat;

/**
 * ExactMatcher
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class ExactMatcher implements CapMat {
    @Override
    public boolean matches(Object requested, Object provided) {
        return requested.equals(provided);
    }
}
