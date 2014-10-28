package com.xing.qa.selenium.grid.hub.capmat;

/**
 * CapMat
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public interface CapMat {

    public boolean matches(Object requested, Object provided);

}
