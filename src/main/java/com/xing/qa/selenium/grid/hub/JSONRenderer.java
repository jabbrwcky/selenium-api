package com.xing.qa.selenium.grid.hub;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSONRenderer
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public interface JSONRenderer {
  JSONObject render() throws JSONException;
}
