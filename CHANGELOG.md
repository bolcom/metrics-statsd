
# CHANGELOG

## 1.4.0

 * Updated DropWizard metrics to 3.2.2
 * Updated SLF4J API to 1.7.25
 * Changed the behaviour of the 'skipUnchangedMetrics' feature. It will now only skip the duration values of timers and the histgram values of a histogram.
   Meters will always be reported and also the count and rates of timers and histgrams will also always be reported. 
 * Added 'skipUnchangedTimerDurationMetrics' method to builder to enable/disable skipping of unchanged duration values specifically for timers.
 * Added 'skipUnchangedHistogramMetrics' method to builder to enable/disable skipping of unchanged histogram values specifically for histograms.
 
   

## 1.3.0

 * By default unchanged timers, meters and histograms won't be send to StatsD anymore. This option can be disabled by setting the 'skipUnchangedMetrics'
   to false on reporter builder.

## 1.2.0

 * Added support for reporting boolean gauges. True is reported as 1 and False is reported as 0. 
  
## 1.1.0

 * Upgraded to Dropwizard Metrics 3.1.2
 * Changed package to 'com.bol.dropwizard.metrics.reporting'
 * Changed groupId to 'com.bol.dropwizard.metrics