package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.RemoteException;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * MonitoringWebProxy
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class MonitoringWebProxy extends DefaultRemoteProxy {

    private static final Logger LOG = Logger.getLogger(MonitoringWebProxy.class.getName());

    private static final String DATABASE = envOr("IFXDB_DB", "selenium-grid");

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(64);

    private static final String URL = format("http://%s:%s", envOr("IFXDB_HOST", "localhost"), envOr("IFXDB_PORT", "8086"));
    private static final InfluxDB INFLUX_DB = InfluxDBFactory.connect(
            URL,
            envOr("IFXDB_USER", "root"),
            envOr("IFXDB_PASSWD", "root"));

    static {
        LOG.info(String.format("Reporting to: %s/db/%s", URL, DATABASE));
        INFLUX_DB.setLogLevel(InfluxDB.LogLevel.NONE);
    }

    private static String envOr(String envVar, String defaultVal) {
        String val = System.getenv(envVar);
        if (val == null) return defaultVal;
        return val;
    }

    private final Logger log = Logger.getLogger(getClass().getName());

    private ScheduledFuture<?> nodeReporter;

    private String remoteHostName;

    public MonitoringWebProxy(RegistrationRequest request, Registry registry) {
        super(request, registry);
        final String addr = request.getConfigAsString("host");
        remoteHostName = addr;

        try {
            remoteHostName = InetAddress.getByName(addr).getCanonicalHostName();
        } catch (Exception e) {
            LOG.info(format("Unable to lookup name for rempote address '%s", addr));
        }

        LOG.info(String.format("Initializing monitoring WebProxy for %s: %s.", remoteHostName, request.toJSON()));

        nodeReporter = EXECUTOR.scheduleAtFixedRate(new NodeReporter(), 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void beforeSession(TestSession session) {
        EXECUTOR.execute(new SessionReporter(session, ReportType.start));
        super.beforeSession(session);
    }

    @Override
    public void afterSession(TestSession session) {
        super.afterSession(session);
        EXECUTOR.execute(new SessionReporter(session, ReportType.finish));
    }

    @Override
    public void onEvent(List<RemoteException> events, RemoteException lastInserted) {
        super.onEvent(events, lastInserted);
        EXECUTOR.execute(new ErrorReporter(lastInserted));
    }

    @Override
    public void startPolling() {
        super.startPolling();
    }

    @Override
    public void stopPolling() {
        super.stopPolling();
    }

    @Override
    public void afterCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
        ContentSnoopingRequest req = new ContentSnoopingRequest(request);
        EXECUTOR.execute(new CommandReporter(session, req, response, ReportType.result));
        super.afterCommand(session, req, response);
    }

    @Override
    public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
        ContentSnoopingRequest req = new ContentSnoopingRequest(request);
        EXECUTOR.execute(new CommandReporter(session, req, response, ReportType.command));
        super.beforeCommand(session, req, response);
    }

    @Override
    public void beforeRelease(TestSession session) {
        super.beforeRelease(session);
        EXECUTOR.execute(new SessionReporter(session, ReportType.timeout));
    }

    private class SessionReporter implements Runnable {
        private final TestSession session;
        private final ReportType type;

        SessionReporter(TestSession session, ReportType type) {
            this.session = session;
            this.type = type;
        }

        @Override
        public void run() {
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

            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, srep.build(), req.build(), prov.build());
        }

    }

    private class NodeReporter implements Runnable {

        @Override
        public void run() {
            LOG.finer(String.format("Reporting: node.%s.measure", SerieNames.utilization));
            int used = getTotalUsed();
            int total = getMaxNumberOfConcurrentTestSessions();
            Serie load = new Serie.Builder(String.format("node.%s.measure", SerieNames.utilization))
                    .columns(
                            "host",
                            "used",
                            "total"
                    ).values(
                            remoteHostName,
                            used,
                            total
                    ).build();
            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, load);
        }
    }

    private class ErrorReporter implements Runnable {
        private final RemoteException exception;

        public ErrorReporter(RemoteException ex) {
            this.exception = ex;
        }

        @Override
        public void run() {
            Serie exRep = new Serie.Builder(SerieNames.node_errors.toString())
                    .columns(
                            "host",
                            "error",
                            "message"
                    )
                    .values(
                            remoteHostName,
                            exception.getClass().getName(),
                            exception.getMessage()
                    ).build();
            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, exRep);
        }

    }

    private class CommandReporter implements Runnable {
        private final TestSession session;
        private final ContentSnoopingRequest request;
        private final HttpServletResponse response;
        private final ReportType type;

        public CommandReporter(TestSession session, ContentSnoopingRequest request, HttpServletResponse response, ReportType type) {
            this.session = session;
            this.request = request;
            this.response = response;
            this.type = type;

        }

        @Override
        public void run() {

            ExternalSessionKey esk = session.getExternalKey();
            String sessionKey = null;
            if (esk != null) {
                sessionKey = esk.getKey();
            }

            try {
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

                INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, s);
            } catch (Exception e) {
                LOG.warning(e.getMessage());
            }
        }

    }

    @Override
    public void teardown() {
        nodeReporter.cancel(false);
    }

    private class ContentSnoopingRequest extends HttpServletRequestWrapper {

        private String content;
        private String encoding;

        public String getContent() {
            return content;
        }

        /**
         * Constructs a request object wrapping the given request.
         *
         * @param request
         * @throws IllegalArgumentException if the request is null
         */
        public ContentSnoopingRequest(HttpServletRequest request) {
            super(request);

            encoding = request.getCharacterEncoding();
            if (encoding == null)
                encoding = "ISO-8859-1";

            try {
                StringBuilder sb = new StringBuilder();
                InputStream is = request.getInputStream();
                byte[] buffer = new byte[1024];
                int read = 0;

                while ((read = is.read(buffer, 0, 1024)) != -1) {
                    sb.append(new String(buffer, 0, read, encoding));
                }

                this.content = sb.toString();

            } catch (IOException e) {
                e.printStackTrace();
                this.content = "";
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {

                int idx = 0;
                byte[] contents = content.getBytes(encoding);

                @Override
                public int read() throws IOException {
                    if (idx < contents.length) {
                        return contents[idx++];
                    } else return -1;
                }
            };
        }

    }
}
