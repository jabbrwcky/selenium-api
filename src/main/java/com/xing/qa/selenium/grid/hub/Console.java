package com.xing.qa.selenium.grid.hub;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.web.Hub;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.Capabilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Console information nad more as JSON
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class Console extends RegistryBasedServlet {

    private final Logger log = Logger.getLogger(getClass().getName());
    private String coreVersion;
    private String coreRevision;

    public Console() {
        this(null);
    }

    public Console(Registry registry) {
        super(registry);
        getVersion();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/requests".equals(req.getPathInfo())) {
                sendJson(pendingRequests(), req, resp);
            } else {
                sendJson(status(), req, resp);
            }
        } catch (JSONException je) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setStatus(500);
            JSONObject error = new JSONObject();

            try {
                error.put("message", je.getMessage());
                error.put("location", je.getStackTrace());
                error.write(resp.getWriter());
            } catch (JSONException e1) {
              log.log(Level.WARNING, "Failed to write error response", e1);
            }

        }

    }

    protected void sendJson(JSONObject jo, HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(200);
        Writer w = null;
        try {
            w = resp.getWriter();
            jo.write(w);
        } catch (IOException e) {
            log.log(Level.WARNING, "Error writing response", e);
        } catch (JSONException e) {
            log.log(Level.WARNING, "Failed to serialize JSON response", e);
        }
    }

    protected JSONObject pendingRequests() throws JSONException {
        JSONObject pending = new JSONObject();
        int p = getRegistry().getNewSessionRequestCount();
        int to = getRegistry().getNewSessionWaitTimeout();
        List<Map<String,?>> desired;

        if (p > 0) {
            desired = new ArrayList<Map<String, ?>>();
            for (Capabilities c: getRegistry().getDesiredCapabilities()) {
                desired.add(c.asMap());
            }
        } else {
            desired = Collections.emptyList();
        }

        pending.put("pending", p);
        pending.put("requested_capabilities", desired);
        pending.put("timeout", to);

        return pending;
    }

    protected JSONObject status()
            throws JSONException {
            JSONObject status = new JSONObject();

            Hub h = getRegistry().getHub();

            List<JSONObject> nodes = new ArrayList<JSONObject>();

            for (RemoteProxy proxy : getRegistry().getAllProxies()) {
                JSONRenderer beta = new WebProxyJsonRenderer(proxy);
                nodes.add(beta.render());
            }

            status.put("version", coreVersion + coreRevision);
            status.put("configuration", getRegistry().getConfiguration().getAllParams());
            status.put("host", h.getHost());
            status.put("port", h.getPort());
            status.put("registration_url", h.getRegistrationURL());
            status.put("nodes", nodes);
            status.put("requests", pendingRequests());

            return status;
    }

    private void getVersion() {
        final Properties p = new Properties();

        InputStream stream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("VERSION.txt");
        if (stream == null) {
            log.severe("Couldn't determine version number");
            return;
        }
        try {
            p.load(stream);
        } catch (IOException e) {
            log.severe("Cannot load version from VERSION.txt" + e.getMessage());
        }

        coreVersion = p.getProperty("selenium.core.version");
        coreRevision = p.getProperty("selenium.core.revision");

        if (coreVersion == null) {
            log.severe("Cannot load selenium.core.version from VERSION.txt");
        }
    }

}
