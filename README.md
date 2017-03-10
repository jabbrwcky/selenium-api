selenium-api
============

Additional Servlets and monitoring for Selenium Grid 2

JSON console
------------

The servlet `com.xing.qa.selenium.grid.hub.Console` implements an endpoint that returns the information given by the
regular console as JSON (and adds some extra infos).

### Usage

```
java -cp selenium-standalone<version>.jar:selenium-api.jar org.openqa.grid.selenium.GridLauncherV3 -servlets com.xing.qa.selenium.grid.hub.Console -role hub 
```

This will add new URL endpoint to the selenium grid hub: `http://localhost:4444/grid/admin/Console/*`

Supported HTTP methods: GET
Returned content type: `application/json`

A call to http://localhost:4444/grid/admin/Console will return a full status report on:

 * version and status of the hub server
 * list of nodes and their installed selenium version and available/utilized browsers

### Detail requests

The call `http://localhost:4444/grid/admin/Console/requests` will just return a list of the pending requests of the connected nodes.

Monitoring
----------

When a node is not started with the default class `DefaultRemoteProxy` but with `-proxy com.xing.qa.selenium.grid.node.MonitoringWebProxy`
the hub will report metrics on the operating system and node operation to a InfluxDB server.

### Configuring reporting

The Configuration of the reporting destination is done by environment variables:

| Variable       | Default         | Description                             |
+----------------+-----------------+-----------------------------------------+
| `IFXDB_HOST`   | `localhost`     | InfluxDB server hostname                |
| `IFXDB_PORT`   | `8086`          | InfluxDB server port                    |
| `IFXDB_DB`     | `selenium-grid` | Name of database for the collected data |
| `IFXDB_USER`   | `root`          | InfluxDB username                       |
| `IFXDB_PASSWD` | `root`          | InfluxDB password                       |

### Reported metrics

The MonitoringWebProxy reports the following series and values to InfluxDB:

| Serie                                   | Content                                                            |
+-----------------------------------------+--------------------------------------------------------------------+
| `node.utilization.measure`              | measures the utilization of available selenium sessions            |
| `node.errors`                           | reports session errors                                             |
| `session.cap.provided.finish.measure`   | tracking of provided capabilities at end of session                |
| `session.cap.provided.start.measure`    | tracking of provided capabilities at selenium session start event  |
| `session.cap.provided.timeout.measure`  | tracking of provided capabilities at session timeout event data    |
| `session.cap.requested.finish.measure`  | tracking of requested capabilities at end of session               |                  
| `session.cap.requested.start.measure`   | tracking of requested capabilities at selenium session start event |    
| `session.cap.requested.timeout.measure` | tracking of requested capabilities at session timeout event data   |      
| `session.cmd.command.measure`           | tracking of sent commands                                          |
| `session.cmd.result.measure`            | tracking of command results                                        |
| `session.event.measure`                 | measuring of selenium events (start, finish, timeout)              |

#### Serie node.utilization.measure

| Field        | Type     | Content                               |
+--------------+----------+---------------------------------------+
| `host`       | `String` | hostname of remote node               |
| `used`       | `int`    | number of used slots at sampling time |
| `total`      | `int`    | number of total available slots       |
| `normalized` | `float`  | normalized usage (used/total) {0..1}  |

#### Serie node.errors

| Field     | Type     | Content                     |
+-----------+----------+-----------------------------+
| `host`    | `String` | hostname of remote node     |
| `error`   | `String` | error class name            |
| `message` | `String` | error message               |

#### Serie session.cap.provided.start.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |


#### Serie session.cap.provided.finish.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |

#### Serie session.cap.provided.timeout.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |


#### Serie session.cap.requested.start.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |

#### Serie session.cap.requested.finish.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |

#### Serie session.cap.requested.timeout.measure

| Field        | Type      | Content                                            |
+--------------+-----------+----------------------------------------------------+
| `host`       | `String`  | hostname of remote node                            |
| `ext_key`    | `String`  | external key of session                            |
| `int_key`    | `String`  | internal key of sesson                             |
| `inactivity` | `long`    | milliseconds of inavctivity                        |
| `forwarding` | `boolean` | true, if forwarding request                        |
| `orphaned`   | `boolean` | true, if orphaned session                          |
| `capability` | `String`  | name of capability                                 |
| `val`        | `any`     | value of capability (String, numerical or boolean) |

#### Serie session.cmd.command.measure

| Field        | Type      | Content                      |
+--------------+-----------+------------------------------+
| `host`       | `String`  | hostname of remote node      |
| `ext_key`    | `String`  | external key of session      |
| `int_key`    | `String`  | internal key of sesson       |
| `inactivity` | `long`    | milliseconds of inavctivity  |
| `forwarding` | `boolean` | true, if forwarding request  |
| `orphaned`   | `boolean` | true, if orphaned session    |
| `cmd_method` | `String`  | HTTP method of command       |
| `cmd_action` | `any`     | Command URL called on remote |
| `cmd`        | `String`  | Request body of command      |

#### Serie session.cmd.result.measure

| Field        | Type      | Content                      |
+--------------+-----------+------------------------------+
| `host`       | `String`  | hostname of remote node      |
| `ext_key`    | `String`  | external key of session      |
| `int_key`    | `String`  | internal key of sesson       |
| `inactivity` | `long`    | milliseconds of inavctivity  |
| `forwarding` | `boolean` | true, if forwarding request  |
| `orphaned`   | `boolean` | true, if orphaned session    |
| `cmd_method` | `String`  | HTTP method of command       |                      
| `cmd_action` | `any`     | Command URL called on remote |                      
| `cmd`        | `String`  | Request body of command      |                    

#### Serie session.event.measure

| Field        | Type      | Content                                        |
+--------------+-----------+------------------------------------------------+
| `host`       | `String`  | hostname of remote node                        |
| `ext_key`    | `String`  | external key of session                        |
| `int_key`    | `String`  | internal key of sesson                         |
| `inactivity` | `long`    | milliseconds of inavctivity                    |
| `forwarding` | `boolean` | true, if forwarding request                    |
| `orphaned`   | `boolean` | true, if orphaned session                      |
| `type`       | `String`  | type of session event (start, finish, timeout) |

Custom Implementations of Selenium components
---------------------------------------------

### Prioritizer

The class `com.xing.qa.selenium.grid.hub.Prioritizer` implements a Session prioritizer for the Selenium hub that
assigns a higher priority to tasks coming from an CI server (see Custom Capabilities for details)

#### Using the prioritizer

Add the following property to your JSON hub configuration file:

```
{
  ...
  "prioritizer": "com.xing.qa.selenium.grid.hub.Prioritizer",
  ...
}
```
### Capability Matcher

The class `com.xing.qa.selenium.grid.hub.ConfigurableCapabilityMatcher` implements a CapabilityMatcher that (w/o further configuration)
mimicks the behaviour of the default capability matcher with a few notable exceptions:

  1. Custom capability matchers can be added and reused for different capabilities.
  2. The version matcher for comparing requested browser versions is more versatile than the exact match offered by the
     default capability matcher.

#### Using the new capability matcher

Add a property capability matcher to your JSON config for configuring the hub:
```
{
  ...
  "capabilityMatcher": "com.xing.qa.selenium.grid.hub.ConfigurableCapabilityMatcher",
  ...
}
```

#### Configuring the capability matcher

the capability matcher is configured via a env variable, `SELENIUM_MATCHERS` that has the following structure:

```
SELENIUM_MATCHERS=<capability>:<matcher name>[,<capability>:<matcher name>]...
```

If no configuration is given, the default mapping is used:

```
SELENIUM_MATCHERS=platform:platform,browserName:exact,version:rvm
```

Any other capabilities may be specified but will not be evaluated by the CapabilityMatcher. Capabilities starting with
an underscore (`_`) are considered to be grid-internal properties and will be generally exempt from matching.

A specified value of `""`, `" "`, `"*"` or anything that translates to `null` will be considered as `ANY` value.

#### Predefined Capability matchers

| Name     | Description                                                           |
+----------+-----------------------------------------------------------------------+
| exact    | Matches if required and provided capability match exactly.            |
| platform | Same behaviour as the default matcher wrt. platform names             |
| rvm      | "Ruby Version Matcher" that compares versions like rubygems does this |

##### Exact matcher

The exact matcher has no further configuration.

##### Platform matcher

The platform matcher has no further configuration. It tries to resolve any version passed in to a valid platform
and tries to match this against the provided capabilities of a node.

##### RVM

Version specification in the requested capabilities.

| spec    | meaning                                                                |
+---------+------------------------------------------------------------------------+
| `x.y`   | Version has to match x.y exactly                                       |
| `~>x.y` | Version matches `x.y`, `x.y.z` excluding version `<x.y` and `>(x+1).y` |
| `>x.y`  | Version matches any version greater than x.y (e.g. `x.(y+1)`           |
| `<x.y`  | Version matches any version less than x.y (e.g. `x.(y-1)`              |
| `>=x.y` | Version matches any version greater than x.y (e.g. `x.(y+1)`           |
| `<=x.y` | Version matches any version greater than x.y (e.g. `(x-1).(y+1)`       |

Any Part of a version that can not be converted to a numerical value will be compared as exact string match (e.g. `x.y.beta`).

#### Implementing additional capability matchers

To implement additional capability matchers you can either extend this project or keep the capability matchers in your own,
separate JAR file.

Implementing a capability matcher consists of two steps:

 1. Implementing the capability matcher.
 2. Providing registration information to make the capability matcher configurable.

##### Implementing the capability matcher

Each capability matcher has to implement the interface `com.xing.qa.selenium.grid.hub.capmat.CapMat`, which is a simple,
single method interface:

```
public interface CapMat {
  public boolean matches(Object requested, Object provided);
}
```

The capability matcher has to provide a no-argument constructor to be usable.

##### Registering the capability matcher

To make the capability matcher available for use, you have to provide a file named `capabilityMatchers` in the root of your
jar/classpath.

The file is a pretty standard Java properties file mapping a name that is used to reference the capability matcher later
on to the class name of the capability matcher to instantiate.

Here is the file content of the file registering the default matchers:

```
rvm: com.xing.qa.selenium.grid.hub.capmat.RubyVersionMatcher
platform: com.xing.qa.selenium.grid.hub.capmat.PlatformMatcher
exact: com.xing.qa.selenium.grid.hub.capmat.ExactMatcher
```

After extending the registration file in this project or adding your external jar containing the file
you can map your matcher to a capability (name) in the Matcher configuration.

Custom capabilities
-------------------

| Property | Value | Description                                           |
+----------+-------+-------------------------------------------------------+
| `_CI`    | bool  | Indicates that the session is coming from a CI server |

Limitations and known issues
----------------------------

 * The configuration is somewhat elaborate to avoid messing with the core selenium configuration. It might be an idea to
   add a configuration file on its own or to extend the selenium node configuration as the configuration by environment
   might hit a limit at some point (esp. with the ConfigurableCapabilityMatcher).
