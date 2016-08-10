
# CHANGELOG

## 1.3.0

 * By default unchanged timers, meters and histograms won't be send to StatsD anymore. This option can be disabled by setting the 'skipUnchangedMetrics'
   to false on reporter builder.

## 1.2.0

 * Added support for reporting boolean gauges. True is reported as 1 and False is reported as 0. 
  
## 1.1.0

 * Upgraded to Dropwizard Metrics 3.1.2
 * Changed package to 'com.bol.dropwizard.metrics.reporting'
 * Changed groupId to 'com.bol.dropwizard.metrics