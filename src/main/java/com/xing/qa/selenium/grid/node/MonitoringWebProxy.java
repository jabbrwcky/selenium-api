package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.RemoteException;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private static final String DATABASE = envOr("IFXDB_DB", "selenium-grid");

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(64);

    private static final InfluxDB INFLUX_DB = InfluxDBFactory.connect(
            format("http://%s:%s", envOr("IFXDB_HOST", "localhost"), envOr("IFXDB_PORT", "8086")),
            envOr("IFXDB_USER", "root"),
            envOr("IFXDB_PASSWD", "root"));

    private static String envOr(String envVar, String defaultVal) {
        String val = System.getenv(envVar);
        if (val == null) return defaultVal;
        return val;
    }

    private final Logger log = Logger.getLogger(getClass().getName());

    private ScheduledFuture<?> nodeReporter;
    private String name;

    public MonitoringWebProxy(RegistrationRequest request, Registry registry) {
        super(request, registry);
        this.name = request.getName();
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
        EXECUTOR.execute(new CommandReporter(session, request, response, ReportType.result));
        super.afterCommand(session, request, response);
    }

    @Override
    public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
        EXECUTOR.execute(new CommandReporter(session, request, response, ReportType.command));
        super.beforeCommand(session, request, response);
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
            Serie.Builder srep = new Serie.Builder(format("session.%s.event.%s.measure", session.getExternalKey().getKey(), type));

            if (ReportType.timeout != type) {
                srep.columns(
                        "host",
                        type.toString(),
                        "ext_key",
                        "int_key",
                        "forwarding",
                        "orphaned",
                        "inactivity"
                );
                srep.values(
                        name,
                        session.getExternalKey().getKey(),
                        session.getInternalKey(),
                        String.valueOf(session.isForwardingRequest()),
                        String.valueOf(session.isOrphaned()),
                        String.valueOf(session.getInactivityTime())
                );
            } else {
                srep.columns("host",
                        type.toString(),
                        "ext_key",
                        "int_key",
                        "forwarding",
                        "orphaned",
                        "inactivity",
                        "browser_starting");
                srep.values(
                        name,
                        session.getExternalKey().getKey(),
                        session.getInternalKey(),
                        String.valueOf(session.isForwardingRequest()),
                        String.valueOf(session.isOrphaned()),
                        String.valueOf(session.getInactivityTime()),
                        String.valueOf(session.getInternalKey() == null)
                );
            }

            Serie.Builder req = new Serie.Builder(
                    format("session.%s.cap.requested.%s.measure", session.getExternalKey().getKey(), type));
            req.columns("host", "ext_key", "int_key", "session", "forwarding", "orphan", "inactivity", "capability", "val");

            Serie.Builder prov = new Serie.Builder(format("session.%s.cap.provided.%s.measure", session.getExternalKey().getKey(), type));
            prov.columns("host", "ext_key", "int_key", "session", "forwarding", "orphan", "inactivity", "capability", "val");

            for (Map.Entry<String, Object> scap : session.getSlot().getCapabilities().entrySet()) {
                prov.values(name, session.getExternalKey().getKey(),
                        session.getInternalKey(),
                        String.valueOf(session.isForwardingRequest()),
                        String.valueOf(session.isOrphaned()),
                        String.valueOf(session.getInactivityTime()),
                        scap.getKey(),
                        String.valueOf(scap.getValue()));
            }

            for (Map.Entry<String, Object> rcap : session.getRequestedCapabilities().entrySet()) {
                req.values(name, session.getExternalKey().getKey(),
                        session.getInternalKey(),
                        String.valueOf(session.isForwardingRequest()),
                        String.valueOf(session.isOrphaned()),
                        String.valueOf(session.getInactivityTime()),
                        rcap.getKey(),
                        String.valueOf(rcap.getValue()));
            }

            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, srep.build(), req.build(), prov.build());
        }

    }

    private class NodeReporter implements Runnable {

        @Override
        public void run() {
            int used = getTotalUsed();
            int total = getMaxNumberOfConcurrentTestSessions();
            Serie load = new Serie.Builder(String.format("node.%s.measure", SerieNames.utilization))
                    .columns("host", "used", "total").values(name, used, total).build();
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
                    .columns("host", "error", "message")
                    .values(name, exception.getClass().getName(), exception.getMessage()).build();
            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, exRep);
        }

    }

    private class CommandReporter implements Runnable {
        private final TestSession session;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final ReportType type;

        public CommandReporter(TestSession session, HttpServletRequest request, HttpServletResponse response, ReportType type) {
            this.session = session;
            this.request = request;
            this.response = response;
            this.type = type;
        }

        @Override
        public void run() {
            Serie s = new Serie.Builder(String.format("session.%s.command.%s.measure", session.getExternalKey().getKey(), type))
                    .columns(
                            "host",
                            type.toString(),
                            "ext_key",
                            "int_key",
                            "forwarding",
                            "orphaned",
                            "inactivity",
                            "cmd_method",
                            "cmd_action"
                    )
                    .values(
                            name,
                            session.getExternalKey().getKey(),
                            session.getInternalKey(),
                            String.valueOf(session.isForwardingRequest()),
                            String.valueOf(session.isOrphaned()),
                            String.valueOf(session.getInactivityTime()),
                            request.getMethod(),
                            request.getPathInfo()
                    )
                    .build();
            INFLUX_DB.write(DATABASE, TimeUnit.MILLISECONDS, s);
        }
    }

    @Override
    public void teardown() {
        nodeReporter.cancel(false);
    }
}
