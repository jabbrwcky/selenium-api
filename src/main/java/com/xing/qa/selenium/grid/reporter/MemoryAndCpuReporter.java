package com.xing.qa.selenium.grid.reporter;

import org.hyperic.sigar.*;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;

import java.util.concurrent.TimeUnit;

/**
 * MemoryAndCpuReporter
 *
 * @author Jens Hausherr (jens.hausherr@xing.com)
 */
public class MemoryAndCpuReporter implements Runnable {

    private final Sigar monitor;
    private final InfluxDB influxDB;

    public MemoryAndCpuReporter(Sigar monitor, InfluxDB influxDB) {
        this.monitor = monitor;
        this.influxDB = influxDB;
    }

    @Override
    public void run() {
        try {
            String fqdn = monitor.getFQDN();

            double[] loadAvg = monitor.getLoadAverage();

            Serie load = new Serie.Builder("load")
                    .columns("host", "l1", "l5", "l15")
                    .values(fqdn, loadAvg[0], loadAvg[1], loadAvg[2])
                    .build();
            influxDB.write("selenium-grid", TimeUnit.MILLISECONDS, load);

            CpuPerc[] cpus = monitor.getCpuPercList();

            Serie.Builder sb = new Serie.Builder("cpu")
                    .columns("host", "cpu", "combined", "idle", "system", "user", "wait");

            for (int i = 0; i < cpus.length; i++) {
                CpuPerc cpu = cpus[0];
                sb.values(fqdn, i, cpu.getCombined(), cpu.getIdle(), cpu.getSys(), cpu.getUser(), cpu.getWait());
            }

            Serie cpuReport = sb.build();

            influxDB.write("selenium-grid", TimeUnit.MILLISECONDS, cpuReport);

            Uptime upt = monitor.getUptime();

            Serie uptime = new Serie.Builder("uptime")
                    .columns("host", "uptime")
                    .values(fqdn, upt.getUptime())
                    .build();
            influxDB.write("selenium-grid", TimeUnit.MILLISECONDS, uptime);

            Mem mem = monitor.getMem();

            Serie memoryReport = new Serie.Builder("memory")
                    .columns("host", "total", "ram", "free", "act_free", "pct_free", "used", "act_used", "pct_used")
                    .values(fqdn, mem.getTotal(), mem.getRam(), mem.getFree(), mem.getActualFree(), mem.getFreePercent(), mem.getUsed(), mem.getActualUsed(), mem.getUsedPercent())
                    .build();
            influxDB.write("selenium-grid", TimeUnit.MILLISECONDS, memoryReport);

        } catch (SigarException e) {
            e.printStackTrace();
        }
    }
}
