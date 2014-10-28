package com.xing.qa.selenium.grid.hub.capmat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capabilities Matcher for Version numbers that accepts Ruby gem version specifications.
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class RubyVersionMatcher implements CapMat {

    private static final Logger LOGGER = Logger.getLogger(RubyVersionMatcher.class.getName());

    private static final Pattern VERSION_SPEC = Pattern.compile("(~>|<=?|>=?)?\\s*((?:\\d+)(?:\\.[^.]+)*)");

    @Override
    public boolean matches(Object requested, Object provided) {
        String prov = String.valueOf(provided);
        Matcher matcher = VERSION_SPEC.matcher(String.valueOf(requested));

        if (matcher.find()) {
            String prefix = matcher.group(1);
            String version = matcher.group(2);

            if (prefix == null) {
                /*exact match*/
                return version.equals(provided);
            }

            if ("~>".equals(prefix)) {
                return compareBounded(version, prov);
            }

            return compare(version, prov, prefix.startsWith(">"), prefix.endsWith("="));
        }

        LOGGER.warning(String.format("Could not parse version specification: '%s'", requested));

        return false;
    }

    private boolean compare(String version, String provided, boolean greaterThan, boolean equalityPermitted) {
        if (version.equals(provided)) {
            return equalityPermitted;
        }

        List<String> req = new ArrayList<String>(Arrays.asList(version.split("\\.")));
        List<String> prov = new ArrayList<String>(Arrays.asList(provided.split("\\.")));

        return compare(req,prov, greaterThan);
    }

    private boolean compare(List<String> req, List<String> prov, boolean greaterThan) {
        if (req.isEmpty() || prov.isEmpty()) return false;

        int c = compareVersion(req.get(0), prov.get(0));

        if (c != 0)
            return ((!greaterThan && c > 0) || (greaterThan && c < 0));

        req.remove(0);
        prov.remove(0);
        return compare(req, prov, greaterThan);

    }

    private boolean compareBounded(String version, String provided) {
        String[] req = version.split("\\.");
        String[] prov = provided.split("\\.");
        if (compareVersion(req[0], prov[0]) != 0)
            return false;

        for (int i = 1; i < req.length; i++) {
            if (compareVersion(req[i], prov[i]) > 0)
                return false;
        }

        return true;
    }

    private int compareVersion(String version, String provided) {
        try {
            Integer rv = Integer.valueOf(version);
            Integer pv = Integer.valueOf(provided);
            return rv.compareTo(pv);
        } catch (NumberFormatException nfe) {
                /* Fall back to exact match if either not parseable as number */
            return version.compareTo(provided);
        }

    }
}
