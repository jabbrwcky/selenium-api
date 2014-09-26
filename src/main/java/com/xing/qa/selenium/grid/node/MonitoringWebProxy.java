package com.xing.qa.selenium.grid.node;

import com.xing.qa.selenium.grid.reporter.MemoryAndCpuReporter;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
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
import java.util.ArrayList;
import java.util.Arrays;
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

    public static final String DATABASE = "selenium-grid";

    private final Logger log = Logger.getLogger(getClass().getName());

    private final ScheduledExecutorService executor;

    private final InfluxDB influxDB;

    private final Sigar systemMonitor;

    private String fqdn;

    private ScheduledFuture<?> systemReporter;
    private ScheduledFuture<?> nodeReporter;

    public MonitoringWebProxy(RegistrationRequest request, Registry registry) {
        super(request, registry);

        executor = Executors.newScheduledThreadPool(32);

        influxDB = InfluxDBFactory.connect(
                format("http://%s:%s", envOr("IFXDB_HOST", "localhost"), envOr("IFXDB_PORT", "8086")),
                envOr("IFXDB_USER", "root"),
                envOr("IFXDB_PASSWD", "root"));

        systemMonitor = new Sigar();

        try {
            fqdn = systemMonitor.getFQDN();
        } catch (SigarException e) {
            log.warning("Could not determine host name.");
        }

        startEnvMonitoring();
        startNodeMonitoring();
    }

    private void startNodeMonitoring() {
        nodeReporter = executor.scheduleAtFixedRate(new NodeReporter(), 0, 10, TimeUnit.SECONDS);
    }

    private void startEnvMonitoring() {
        systemReporter = executor.scheduleAtFixedRate(new MemoryAndCpuReporter(systemMonitor, influxDB), 0, 10, TimeUnit.SECONDS);
    }

    private String envOr(String envVar, String defaultVal) {
        String val = System.getenv(envVar);
        if (val == null) return defaultVal;
        return val;
    }

    @Override
    public void beforeSession(TestSession session) {
        executor.execute(new SessionReporter(session, ReportType.start));
        super.beforeSession(session);
    }

    @Override
    public void afterSession(TestSession session) {
        super.afterSession(session);
        executor.execute(new SessionReporter(session, ReportType.finish));
    }

    @Override
    public void onEvent(List<RemoteException> events, RemoteException lastInserted) {
        super.onEvent(events, lastInserted);
        executor.execute(new ErrorReporter(lastInserted));
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
        executor.execute(new CommandReporter(session, request, response, ReportType.result));
        super.afterCommand(session, request, response);
    }

    @Override
    public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
        executor.execute(new CommandReporter(session, request, response, ReportType.command));
        super.beforeCommand(session, request, response);
    }

    @Override
    public void beforeRelease(TestSession session) {
        super.beforeRelease(session);
        executor.execute(new SessionReporter(session, ReportType.timeout));
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
            List<String> columns = new ArrayList<String>(Arrays.asList(
                    "host",
                    type.toString(),
                    "ext_key",
                    "int_key",
                    "forwarding",
                    "orphaned",
                    "inactivity"));

            if (ReportType.timeout == type) {
                columns.add("browser_starting");
            }

            List<String> values = new ArrayList<String>(Arrays.asList(
                    fqdn,
                    session.getExternalKey().getKey(),
                    session.getInternalKey(),
                    String.valueOf(session.isForwardingRequest()),
                    String.valueOf(session.isOrphaned()),
                    String.valueOf(session.getInactivityTime())));

            if (ReportType.timeout == type) {
                values.add(String.valueOf(session.getInternalKey() == null));
            }

            for (Map.Entry<String, Object> scap : session.getSlot().getCapabilities().entrySet()) {
                columns.add("nod_" + scap.getKey());
                values.add(String.valueOf(scap.getValue()));
            }

            for (Map.Entry<String, Object> rcap : session.getRequestedCapabilities().entrySet()) {
                columns.add("req_" + rcap.getKey());
                values.add(String.valueOf(rcap.getValue()));
            }

            Serie s = new Serie.Builder(SerieNames.session.toString())
                    .columns(columns.toArray(new String[columns.size()]))
                    .values(values.toArray())
                    .build();

            influxDB.write(DATABASE, TimeUnit.MILLISECONDS, s);
        }

    }

    private class NodeReporter implements Runnable {

        @Override
        public void run() {
            String fqdn;
            try {
                fqdn = systemMonitor.getFQDN();

                int used = getTotalUsed();
                int total = getMaxNumberOfConcurrentTestSessions();
                double usage = (double) used / (double) total;
                Serie load = new Serie.Builder(SerieNames.utilization.toString())
                        .columns("host", "used", "total", "pct").values(fqdn, used, total, usage).build();

                influxDB.write(DATABASE, TimeUnit.MILLISECONDS, load);

            } catch (SigarException e) {
                e.printStackTrace();
            }
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
                    .values(fqdn, exception.getClass().getName(), exception.getMessage()).build();
            influxDB.write(DATABASE, TimeUnit.MILLISECONDS, exRep);
        }
    }

    private class CommandReporter implements Runnable {
        private final TestSession session;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final ReportType type;

        public CommandReporter(TestSession session, HttpServletRequest request, HttpServletResponse response, ReportType type) {
            this.session=session;
            this.request=request;
            this.response=response;
            this.type=type;
        }

        @Override
        public void run() {
            Serie s = new Serie.Builder(SerieNames.session.toString())
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
                            fqdn,
                            session.getExternalKey().getKey(),
                            session.getInternalKey(),
                            String.valueOf(session.isForwardingRequest()),
                            String.valueOf(session.isOrphaned()),
                            String.valueOf(session.getInactivityTime()),
                            request.getMethod(),
                            request.getPathInfo()
                    )
                    .build();

            influxDB.write(DATABASE, TimeUnit.MILLISECONDS, s);
        }
    }
}
