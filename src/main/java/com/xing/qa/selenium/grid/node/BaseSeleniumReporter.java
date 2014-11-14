package com.xing.qa.selenium.grid.node;

import com.xing.qa.selenium.grid.Version;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.TestSession;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * BaseSeleniumReporter
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public abstract class BaseSeleniumReporter implements Runnable {

    private static final String version = Version.version();

    protected final String remoteHostName;
    protected final Logger log = Logger.getLogger(getClass().getName());
    private final String database;
    private final InfluxDB influxdb;

    public BaseSeleniumReporter(String remoteHostName, InfluxDB influxdb, String database) {
        this.remoteHostName = remoteHostName;
        this.influxdb = influxdb;
        this.database = database;
    }

    @Override
    public final void run() {
        try {
            report();
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
    }

    protected abstract void report();

    protected void write(TimeUnit precision, Serie... series) {
        influxdb.write(database, precision, series);
    }
}
