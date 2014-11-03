package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * CommandReporter
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
class CommandReporter extends BaseSeleniumReporter {

    protected final TestSession session;
    protected final ContentSnoopingRequest request;
    protected final HttpServletResponse response;
    protected final ReportType type;

    public CommandReporter(String remoteHostName, InfluxDB influxdb, String database, TestSession session, ContentSnoopingRequest request, HttpServletResponse response, ReportType type) {
        super(remoteHostName, influxdb, database);
        this.type = type;
        this.request = request;
        this.session = session;
        this.response = response;
    }

    protected void report() {
        ExternalSessionKey esk = session.getExternalKey();
        String sessionKey = null;
        if (esk != null) {
            sessionKey = esk.getKey();
        }

        Serie s = new Serie.Builder(String.format("session.cmd.%s.measure", type))
                .columns(
                        "host",
                        "ext_key",
                        "int_key",
                        "forwarding",
                        "orphaned",
                        "inactivity",
                        "cmd_method",
                        "cmd_action",
                        "cmd"
                )
                .values(
                        remoteHostName,
                        sessionKey,
                        session.getInternalKey(),
                        String.valueOf(session.isForwardingRequest()),
                        String.valueOf(session.isOrphaned()),
                        String.valueOf(session.getInactivityTime()),
                        request.getMethod(),
                        request.getPathInfo(),
                        request.getContent()
                )
                .build();

        write(TimeUnit.MILLISECONDS, s);
    }
}
