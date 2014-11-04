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

Custom Implementations
----------------------

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

| name | desc                       |
+------+----------------------------+
| exact | Matches if required and provided capability match exactly. |
| platform | Same behaviour as the default matcher wrt. platform names |
| rvm | "Ruby Version Matcher" that compares versions like rubygems does this |
 
##### RVM 

Version specification

| spec    | meaning |
+---------+---------+
| `x.y`   | Version has to match x.y exactly |
| `~>x.y` | Version matches x.y, x.y.z excluding version <x.y and > (x+1).y |
| `>x.y`  | TBD |
| `<x.y`  | TBD |
| `>=x.y` | TBD |
| `<=x.y` | TBD | 


#### Extending the capability matcher

TBD


Custom capabilities
-------------------
 
| Property | Value | Description |
+----------+-------+-------------+
| `_CI`    | bool  | Indicates that the session is coming from a CI server |




