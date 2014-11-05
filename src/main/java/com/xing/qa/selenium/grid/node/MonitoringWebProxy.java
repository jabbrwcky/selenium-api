package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.common.exception.RemoteException;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.util.List;
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
        remoteHostName = addr.toLowerCase();

        try {
            remoteHostName = InetAddress.getByName(addr).getCanonicalHostName().toLowerCase();
        } catch (Exception e) {
            LOG.info(format("Unable to lookup name for remote address '%s", addr));
        }

        LOG.info(String.format("Initializing monitoring WebProxy for %s: %s.", remoteHostName, request.toJSON()));

        nodeReporter = EXECUTOR.scheduleAtFixedRate(new NodeReporter(remoteHostName, INFLUX_DB, DATABASE, this), 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void beforeSession(TestSession session) {
        EXECUTOR.execute(new SessionReporter(remoteHostName, INFLUX_DB, DATABASE, session, ReportType.start));
        super.beforeSession(session);
    }

    @Override
    public void afterSession(TestSession session) {
        super.afterSession(session);
        EXECUTOR.execute(new SessionReporter(remoteHostName, INFLUX_DB, DATABASE, session, ReportType.finish));
    }

    @Override
    public void onEvent(List<RemoteException> events, RemoteException lastInserted) {
        super.onEvent(events, lastInserted);
        EXECUTOR.execute(new ErrorReporter(remoteHostName, INFLUX_DB, DATABASE, lastInserted));
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
        EXECUTOR.execute(new CommandReporter(remoteHostName, INFLUX_DB, DATABASE, session, req, response, ReportType.result));
        super.afterCommand(session, req, response);
    }

    @Override
    public void beforeCommand(TestSession session, HttpServletRequest request, HttpServletResponse response) {
        ContentSnoopingRequest req = new ContentSnoopingRequest(request);
        EXECUTOR.execute(new CommandReporter(remoteHostName, INFLUX_DB, DATABASE, session, req, response, ReportType.command));
        super.beforeCommand(session, req, response);
    }

    @Override
    public void beforeRelease(TestSession session) {
        super.beforeRelease(session);
        EXECUTOR.execute(new SessionReporter(remoteHostName, INFLUX_DB, DATABASE, session, ReportType.timeout));
    }

    @Override
    public void teardown() {
        nodeReporter.cancel(false);
    }

}
