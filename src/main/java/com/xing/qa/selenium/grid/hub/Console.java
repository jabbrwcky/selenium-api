package com.xing.qa.selenium.grid.hub;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Console
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class Console extends RegistryBasedServlet {

    private final Logger log = Logger.getLogger(getClass().getName());
    private String coreVersion;
    private String coreRevision;

    public Console(Registry registry) {
        super(registry);
        getVersion();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
        if ("application/json".equals(req.getHeader("Accept"))) {
            processApi(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Only 'application/json' supported!");
        }
    }

    protected void processApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            JSONObject status = new JSONObject();
            status.put("version", coreVersion + coreRevision);
            List<JSONObject> nodes = new ArrayList<JSONObject>();

            for (RemoteProxy proxy : getRegistry().getAllProxies()) {
                JSONRenderer beta = new WebProxyJsonRenderer(proxy);
                nodes.add(beta.render());
            }

            status.put("nodes", nodes);
            response.setStatus(200);
            Writer w = response.getWriter();
            status.write(w);
        } catch (JSONException e) {
            response.setStatus(500);
            JSONObject error = new JSONObject();
            try {
                error.put("message", e.getMessage());
                error.put("location", e.getStackTrace());
                error.write(response.getWriter());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
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
