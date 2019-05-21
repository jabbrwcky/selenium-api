package com.xing.qa.selenium.grid.hub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.InputStream;
import java.util.*;

/**
 * Renderer that presents data on a WebProxy as JSON object.
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class WebProxyJsonRenderer implements JSONRenderer {

    private final RemoteProxy proxy;

    public WebProxyJsonRenderer(RemoteProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public JSONObject render() throws JSONException {
        JSONObject json = new JSONObject();
        Map object = proxy.getProxyStatus();
        Map value = (Map) object.get("value");
        Map build = (Map) value.get("build");
        Map java = (Map) value.get("java");
        json.put("class", proxy.getClass().getSimpleName());

        try {
            json.put("version", build.get("version"));
        } catch (JSONException e) {
            json.put("version", "unknown");
            json.put("error", e.getMessage());
            json.put("trace", e.getStackTrace());
            e.printStackTrace();
        }

        json.put("os",  value.get("os"));
        json.put("java", java.get("version"));
        json.put("configuration", proxy.getConfig());

        SlotsLines rcLines = new SlotsLines();
        SlotsLines wdLines = new SlotsLines();

        for (TestSlot slot : proxy.getTestSlots()) {
            if (slot.getProtocol() == SeleniumProtocol.Selenium) {
                rcLines.add(slot);
            } else {
                wdLines.add(slot);
            }
        }

        JSONObject protocols = new JSONObject();

        if (rcLines.getLinesType().size() != 0) {
            JSONObject prot = new JSONObject();
            prot.put("name", SeleniumProtocol.Selenium);
            prot.put("browsers", getLines(rcLines));
            protocols.put("remote_control", prot);
        }

        if (wdLines.getLinesType().size() != 0) {
            JSONObject prot = new JSONObject();
            prot.put("name", SeleniumProtocol.WebDriver);
            prot.put("browsers", getLines(wdLines));
            protocols.put("web_driver", prot);
        }

        json.put("protocols", protocols);

        return json;
    }

    private JSONObject render(TestSlot slot) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("capabilities", slot.getCapabilities());
        TestSession session = slot.getSession();

        if (session != null) {
            json.put("session", render(session));
            json.put("busy", true);
        } else {
            json.put("busy", false);
        }

        return json;
    }

    private JSONObject render(TestSession session) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("key", session.getExternalKey().getKey());
        json.put("inactivity_time", session.getInactivityTime());
        json.put("requested_capabilities", session.getRequestedCapabilities());
        json.put("forwarding_request", session.isForwardingRequest());
        json.put("orphaned", session.isOrphaned());
        return json;
    }

    // the lines of icon representing the possible slots
    private JSONObject getLines(SlotsLines lines) throws JSONException {
        JSONObject result = new JSONObject();

        for (MiniCapability cap : lines.getLinesType()) {
            if(lines.getLine(cap).isEmpty()) {
                result.put("Nodes","isEmpty");
            }
            for (TestSlot s : lines.getLine(cap)) {
                String browserName = s.getCapabilities().get(CapabilityType.BROWSER_NAME).toString().replaceAll("\\s+", "");
                JSONObject browserObj;

                if (result.has(browserName)) {
                    browserObj = result.getJSONObject(browserName);
                } else {
                    browserObj = new JSONObject();
                    String icon = cap.getIcon();
                    browserObj.put("icon", icon);
                    browserObj.put("name", browserName);
                    result.put(browserName, browserObj);
                }

                String version = cap.getVersion();
                if (version == null) version = "0";
                JSONArray versionArray;

                if (browserObj.has(version)) {
                    versionArray = browserObj.getJSONArray(version);
                } else {
                    versionArray = new JSONArray();
                    browserObj.put(version, versionArray);
                }

                versionArray.put(render(s));
            }
        }
        return result;
    }

}

class MiniCapability {
    private String browser;
    private String version;
    private DesiredCapabilities capabilities;
  
    MiniCapability(TestSlot slot) {
      DesiredCapabilities cap = new DesiredCapabilities(slot.getCapabilities());
      browser = cap.getBrowserName().replaceAll("\\s+","");
      version = cap.getVersion();
      capabilities = cap;
    }
  
    public String getVersion() {
      return version;
    }
  
    public String getIcon() {
      return getConsoleIconPath(new DesiredCapabilities(capabilities));
    }
  
    /**
     * get the icon representing the browser for the grid. If the icon cannot be located, returns
     * null.
     *
     * @param cap - Capability
     * @return String with path to icon image file.  Can be <i>null</i> if no icon
     *         file if available.
     */
    private String getConsoleIconPath(DesiredCapabilities cap) {
      String name = consoleIconName(cap);
      String path = "org/openqa/grid/images/";
      InputStream in =
          Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(path + name + ".png");
      if (in == null) {
        return null;
      }
      return "/grid/resources/" + path + name + ".png";
    }
  
    private String consoleIconName(DesiredCapabilities cap) {
      String browserString = cap.getBrowserName();
      if (browserString == null || "".equals(browserString)) {
        return "missingBrowserName";
      }
  
      String ret = browserString;
  
      // Map browser environments to icon names.
      if (browserString.contains("iexplore") || browserString.startsWith("*iehta")) {
        ret = BrowserType.IE;
      } else if (browserString.contains("firefox") || browserString.startsWith("*chrome")) {
        if (cap.getVersion() != null && cap.getVersion().toLowerCase().equals("beta") ||
            cap.getBrowserName().toLowerCase().contains("beta")) {
          ret = "firefoxbeta";
        } else if (cap.getVersion() != null && cap.getVersion().toLowerCase().equals("aurora") ||
                   cap.getBrowserName().toLowerCase().contains("aurora")) {
          ret = "aurora";
        } else if (cap.getVersion() != null && cap.getVersion().toLowerCase().equals("nightly") ||
                   cap.getBrowserName().toLowerCase().contains("nightly")) {
          ret = "nightly";
        } else {
          ret = BrowserType.FIREFOX;
        }
  
      } else if (browserString.startsWith("*safari")) {
        ret = BrowserType.SAFARI;
      } else if (browserString.startsWith("*googlechrome")) {
        ret = BrowserType.CHROME;
      } else if (browserString.startsWith("opera")) {
        ret = BrowserType.OPERA_BLINK;
      } else if (browserString.toLowerCase().contains("edge")) {
        ret = BrowserType.EDGE;
      }
  
      return ret.replace(" ", "_");
    }
  
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((browser == null) ? 0 : browser.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
    }
  
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MiniCapability other = (MiniCapability) obj;
      if (browser == null) {
        if (other.browser != null) return false;
      } else if (!browser.equals(other.browser)) return false;
      if (version == null) {
        if (other.version != null) return false;
      } else if (!version.equals(other.version)) return false;
      return true;
    }
  
  
  
  }


class SlotsLines {
    private Map<MiniCapability, List<TestSlot>> slots = new HashMap<>();
  
    public void add(TestSlot slot) {
      MiniCapability c = new MiniCapability(slot);
      List<TestSlot> l = slots.computeIfAbsent(c, k -> new ArrayList<>());
      l.add(slot);
    }
  
    public Set<MiniCapability> getLinesType() {
      return slots.keySet();
    }
  
    public List<TestSlot> getLine(MiniCapability cap) {
      return slots.get(cap);
    }
  }
  