package com.xing.qa.selenium.grid.hub;

import com.xing.qa.selenium.grid.hub.capmat.CapMat;
import com.xing.qa.selenium.grid.hub.capmat.ExactMatcher;
import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.selenium.remote.CapabilityType;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Configurable capability matcher.
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class ConfigurableCapabilityMatcher implements CapabilityMatcher {

    private static final Logger LOGGER = Logger.getLogger(ConfigurableCapabilityMatcher.class.getName());

    private static final String DEFAULT_CAPABILITIES = CapabilityType.PLATFORM_NAME + ":platform," + CapabilityType.BROWSER_NAME + ":exact," + CapabilityType.VERSION + ":rvm";
    private static final String GRID_TOKEN = "_";

    private static final Pattern MEH = Pattern.compile("|\\*|[Aa][Nn][Yy]");

    private final Map<String, CapMat> matchedCapabilities = new HashMap<String, CapMat>();

    public ConfigurableCapabilityMatcher() {
        String conf = System.getenv("SELENIUM_MATCHERS");

        if (conf == null)
            conf = DEFAULT_CAPABILITIES;

        LOGGER.config("Configured Capability matching: " + conf);

        System.out.println("Configured Capability matching: " + conf);

        String[] confs = conf.split(",");
        Map<String, Class<? extends CapMat>> matchers = new HashMap<String, Class<? extends CapMat>>();

        try {
            Enumeration<URL> cms = getClass().getClassLoader().getResources("capabilityMatchers");
            System.out.println(cms.hasMoreElements());

            while (cms.hasMoreElements()) {


                Properties p = new Properties();

                try {
                    p.load(cms.nextElement().openStream());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                for (Map.Entry<Object, Object> e : p.entrySet()) {
                    try {
                        matchers.put(String.valueOf(e.getKey()), (Class<CapMat>) Class.forName(String.valueOf(e.getValue())));
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String cnf : confs) {
            String mClass;
            String[] cf = cnf.split(":");

            String cap = cf[0];

            if (cf.length == 1) {
                mClass = "exact";
            } else {
                mClass = cf[1];
            }

            try {
                Class<? extends CapMat> m = matchers.get(mClass);
                if (m == null) m = ExactMatcher.class;
                matchedCapabilities.put(cap, matchers.get(mClass).newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println(cap + "" + mClass);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            for (Map.Entry e : matchedCapabilities.entrySet()) {
                LOGGER.fine(String.format("Capability: %s => Matcher: %s", e.getKey(), e.getValue().getClass().getName()));
            }
        }
    }

    @Override
    public boolean matches(Map<String, Object> nodeCapability, Map<String, Object> requestedCapability) {

        if (nodeCapability == null || requestedCapability == null) {
            return false;
        }

        boolean result = true;

        for (String key : requestedCapability.keySet()) {
            if (key.startsWith(GRID_TOKEN)) continue;

            if (matchedCapabilities.containsKey(key)) {
                if (requestedCapability.get(key) != null) {
                    Object value = requestedCapability.get(key);
                    if (value != null) {
                        if (!MEH.matcher(String.valueOf(value)).matches()) {
                            result = matchedCapabilities.get(key).matches(value, nodeCapability.get(key));
                        }
                    }
                }
            }

            if (!result) break;
        }

        return result;
    }

}
