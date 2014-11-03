package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;

import java.util.concurrent.TimeUnit;

/**
* NodeReporter
*
* @author Jens Hausherr (jens.hausherr@xing.com)
*/
class NodeReporter extends BaseSeleniumReporter {
    private final int used;
    private final int total;

    public NodeReporter(String remoteHostName, InfluxDB influxdb, String database, int used, int total) {
        super(remoteHostName, influxdb, database);
        this.used = used;
        this.total = total;
    }

    @Override
    protected void report() {
        log.finer(String.format("Reporting: node.%s.measure", SerieNames.utilization));
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
        write(TimeUnit.MILLISECONDS, load);
    }
}
