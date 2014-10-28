package com.xing.qa.selenium.grid.hub.capmat;

import org.openqa.selenium.Platform;

import java.util.logging.Logger;

/**
 * PlatformCapabilityMatcher
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class PlatformMatcher implements CapMat {

    private static final Logger LOGGER = Logger.getLogger(PlatformMatcher.class.getName());

    @Override
    public boolean matches(Object requested, Object provided) {
        Platform requestedPlatform = extractPlatform(requested);

        if (requestedPlatform != null) {
            Platform node = extractPlatform(provided);

            if (node == null) {
                return false;
            }
            if (!node.is(requestedPlatform)) {
                return false;
            }
        } else {
          LOGGER.warning(String.format("Unable to extract requested platform from '%s'.",requested));
        }

        return true;
    }

    /**
     * Resolves a platform capability to a Platform instance.
     *
     * Taken from DefaultCapabilityMatcher with small modifications.
     *
     * @param o Object to resolve to a Platform
     *
     * @return Resolved Platform instance or <code>null</code>.
     */
    Platform extractPlatform(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Platform) {
            return (Platform) o;
        } else if (o instanceof String) {
            String name = o.toString();
            try {
                return Platform.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                // no exact match, continue to look for a partial match
            }
            for (Platform os : Platform.values()) {
                for (String matcher : os.getPartOfOsName()) {
                    if ("".equals(matcher))
                        continue;
                    if (name.equalsIgnoreCase(matcher)) {
                        return os;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }
}
