package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

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

    private static WeakHashMap<String, Long> runningCommands = new WeakHashMap<String, Long>();

    public CommandReporter(String remoteHostName, InfluxDB influxdb, String database, TestSession session, ContentSnoopingRequest request, HttpServletResponse response, ReportType type) {
        super(remoteHostName, influxdb, database);
        this.type = type;
        this.request = request;
        this.session = session;
        this.response = response;
    }

    private String hashCommand() {
        try {
            Charset utf8 = Charset.forName("UTF-8");
            MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(session.getInternalKey().getBytes(utf8));
            md.update(request.getMethod().getBytes(utf8));
            md.update(request.getPathInfo().getBytes(utf8));
            md.update(request.getContent().getBytes(utf8));
            return new String(md.digest(), utf8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected void report() {
        ExternalSessionKey esk = session.getExternalKey();
        String sessionKey = null;
        if (esk != null) {
            sessionKey = esk.getKey();
        }

        long time = System.currentTimeMillis();
        Serie execution = null;
        if (type == ReportType.command) {
            runningCommands.put(hashCommand(), time);
        } else {
            Long start = runningCommands.remove(hashCommand());
            long elapsed = -1l;

            if (start != null) {
                elapsed = time - start;
            } else {
                log.warning(format("No start event found for command %s %s: %s",
                        request.getMethod(),
                        request.getPathInfo(),
                        request.getContent()));
            }

            execution = new Serie.Builder("session.cmd.duration.measure")
                    .columns(
                            "time",
                            "host",
                            "ext_key",
                            "int_key",
                            "value",
                            "cmd_method",
                            "cmd_action",
                            "cmd"
                    )
                    .values(
                            time,
                            remoteHostName,
                            sessionKey,
                            session.getInternalKey(),
                            elapsed,
                            request.getMethod(),
                            request.getPathInfo(),
                            request.getContent()

                    ).build();
        }
        write(TimeUnit.MILLISECONDS, execution);
    }

}
