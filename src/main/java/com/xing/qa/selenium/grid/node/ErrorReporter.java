package com.xing.qa.selenium.grid.node;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.openqa.grid.common.exception.RemoteException;
import org.openqa.grid.internal.TestSession;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
* ErrorReporter
*
* @author Jens Hausherr (jens.hausherr@xing.com)
*/
class ErrorReporter extends BaseSeleniumReporter {

    private final RemoteException exception;

    public ErrorReporter(String remoteHostName, InfluxDB influxdb, String database, RemoteException ex) {
        super(remoteHostName, influxdb, database);
        this.exception = ex;
    }

    @Override
    protected void report() {
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
        write(TimeUnit.MILLISECONDS, exRep);
    }

}
