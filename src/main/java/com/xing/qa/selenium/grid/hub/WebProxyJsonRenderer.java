package com.xing.qa.selenium.grid.hub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.SeleniumProtocol;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.web.servlet.beta.MiniCapability;
import org.openqa.grid.web.servlet.beta.SlotsLines;
import org.openqa.selenium.remote.CapabilityType;

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
        JSONObject status = proxy.getStatus();
        json.put("class", proxy.getClass().getSimpleName());

        try {
            json.put("version",
               status.getJSONObject("value").getJSONObject("build").getString("version"));
        } catch (JSONException e) {
            json.put("version", "unknown");
            json.put("error", e.getMessage());
            json.put("trace", e.getStackTrace());
            e.printStackTrace();
        }

    json.put("os", status.getJSONObject("value").getJSONObject("os"));
    json.put("java", status.getJSONObject("value").getJSONObject("java").getString("version"));
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

            for (TestSlot s : lines.getLine(cap)) {
                String browserName = (String) s.getCapabilities().get(CapabilityType.BROWSER_NAME).toString().replaceFirst("\\*", "");
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
