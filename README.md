selenium-api
============

Additional Servlets and monitoring for Selenium Grid 2

JSON console
------------

The servlet `com.xing.qa.selenium.grid.hub.Console` implements an endpoint that returns the information given by the 
regular console as JSON (and adds some extra infos).

### Usage
```
java -cp selenium-standalone<version>.jar:selenium-api.jar org.openqa.grid.selenium.GridLauncher -servlets com.xing.qa.selenium.grid.hub.Console -role hub 
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

| Variable | Default | Description |
+----------+---------+--------------------------------------+
| IFX_DB_HOST |

### Reported metrics

The MonitoringWebProxy reports the following series ans values to InfluxDB

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

Custom capabilities
-------------------
 
| Property | Value | Description |
+----------+-------+-------------+
| `_CI`    | bool  | Indicates that the session is coming from a CI server |




