package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
* SessionReporter
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

        Serie.Builder srep = new Serie.Builder(format("session.event.%s.measure", type));

        final String forwardingRequest = String.valueOf(session.isForwardingRequest());
        final String orphaned = String.valueOf(session.isOrphaned());
        final String inactivityTime = String.valueOf(session.getInactivityTime());
        if (ReportType.timeout != type) {
            srep.columns(
                    "host",
                    "ext_key",
                    "int_key",
                    "forwarding",
                    "orphaned",
                    "inactivity"
            );
            srep.values(
                    remoteHostName,
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime
            );
        } else {
            srep.columns(
                    "host",
                    "ext_key",
                    "int_key",
                    "forwarding",
                    "orphaned",
                    "inactivity",
                    "browser_starting");
            srep.values(
                    remoteHostName,
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
                "host",
                "ext_key",
                "int_key",
                "forwarding",
                "orphan",
                "inactivity",
                "capability",
                "val");

        for (Map.Entry<String, Object> rcap : session.getRequestedCapabilities().entrySet()) {
            req.values(
                    remoteHostName,
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime,
                    rcap.getKey(),
                    String.valueOf(rcap.getValue()));
        }

        Serie.Builder prov = new Serie.Builder(format("session.cap.provided.%s.measure", type));
        prov.columns(
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
                    remoteHostName,
                    sessionKey,
                    session.getInternalKey(),
                    forwardingRequest,
                    orphaned,
                    inactivityTime,
                    scap.getKey(),
                    String.valueOf(scap.getValue()));
        }

        write(TimeUnit.MILLISECONDS, srep.build(), req.build(), prov.build());

    }

}
