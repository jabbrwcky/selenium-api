package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Session Reporter
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
class SessionReporter extends BaseSeleniumReporter {
    private final TestSession session;
    private final ReportType type;

    public SessionReporter(String remoteHostName, InfluxDB influxdb, String database, TestSession session, ReportType type) {
        super(remoteHostName, influxdb, database);
        this.session = session;
        this.type = type;
    }

    @Override
    protected void report() {
        ExternalSessionKey esk = session.getExternalKey();
        String sessionKey = null;

        if (esk != null) {
            sessionKey = esk.getKey();
        }

        Serie.Builder srep = new Serie.Builder("session.event.measure");

        final Boolean forwardingRequest = session.isForwardingRequest();
        final Boolean orphaned = session.isOrphaned();
        final Long inactivityTime = session.getInactivityTime();
        final long time = System.currentTimeMillis();
        if (ReportType.timeout != type) {
            srep.columns(
                    "time",
                    "host",
                    "type",
                    "ext_key",
                    "int_key",
                    "forwarding",
                    "orphaned",
                    "inactivity"
            );
            srep.values(
                    time,
                    remoteHostName,
                    type.toString(),
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime
            );
        } else {
            srep.columns(
                    "time",
                    "host",
                    "type",
                    "ext_key",
                    "int_key",
                    "forwarding",
                    "orphaned",
                    "inactivity",
                    "browser_starting");
            srep.values(
                    time,
                    remoteHostName,
                    type.toString(),
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime,
                    String.valueOf(session.getInternalKey() == null)
            );
        }

        Serie.Builder req = new Serie.Builder(
                format("session.cap.requested.%s.measure", type));
        req.columns(
                "time",
                "host",
                "ext_key",
                "int_key",
                "forwarding",
                "orphan",
                "inactivity",
                "capability",
                "val");

        Map<String,Object> requested = session.getRequestedCapabilities();

        for (Map.Entry<String, Object> rcap : requested.entrySet()) {
            req.values(
                    time,
                    remoteHostName,
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime,
                    rcap.getKey(),
                    rcap.getValue());
        }

        Serie.Builder prov = new Serie.Builder(format("session.cap.provided.%s.measure", type));
        prov.columns(
                "time",
                "host",
                "ext_key",
                "int_key",
                "forwarding",
                "orphan",
                "inactivity",
                "capability",
                "val");

        for (Map.Entry<String, Object> scap : session.getSlot().getCapabilities().entrySet()) {
            prov.values(
                    time,
                    remoteHostName,
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime,
                    scap.getKey(),
                    scap.getValue());
        }

        Serie.Builder sessionProfile = new Serie.Builder("session.req.profile.measure");
        sessionProfile.columns(
                "time",
                "host",
                "ext_key",
                "int_key",
                "id",
                "platform",
                "browserName",
                "version",
                "firefox_binary",
                "firefox_profile",
                "javascriptEnabled",
                "takesScreenshot",
                "nativeEvents",
                "cssSelectorsEnabled",
                "rotatable"
        ).values(
                time,
                remoteHostName,
                sessionKey,
                session.getInternalKey(),
                requested.get("id"),
                requested.get("platform"),
                requested.get("browserName"),
                requested.get("version"),
                requested.get("firefox_profile"),
                requested.get("firefox_binary"),
                requested.get("javascriptEnabled"),
                requested.get("takesScreenshot"),
                requested.get("nativeEvents"),
                requested.get("cssSelectorsEnabled"),
                requested.get("rotatable")
        );

        Serie.Builder nodeProfile = new Serie.Builder("session.prov.profile.measure");
        sessionProfile.columns(
                "time",
                "host",
                "ext_key",
                "int_key",
                "id",
                "platform",
                "browserName",
                "version",
                "firefox_binary",
                "seleniumProtocol",
                "maxInstances"
        ).values(
                time,
                remoteHostName,
                sessionKey,
                session.getInternalKey(),
                requested.get("id"),
                requested.get("platform"),
                requested.get("browserName"),
                requested.get("version"),
                requested.get("firefox_binary"),
                requested.get("seleniumProtocol"),
                requested.get("maxInstances")
        );

        write(TimeUnit.MILLISECONDS, srep.build(), req.build(), prov.build(), sessionProfile.build(), nodeProfile.build());
    }

}
