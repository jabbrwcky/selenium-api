selenium-api
============

Additional Servlets for Selenium Grid 2

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

The call `http://localhost:4444/grid/admin/Console/requests` will just return a list of the pending requests of the 

 



