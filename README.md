# metrics-statsd

Statsd reporter for [dropwizard/metrics] (http://metrics.dropwizard.io/), based on Sean Laurent's [metrics-statsd] (https://github.com/organicveggie/metrics-statsd),  Mike Keesey's [metrics-statsd] (https://github.com/ReadyTalk/metrics-statsd) and  Hannes Heijkenskjöld's [metrics-statsd] (https://github.com/jjagged/metrics-statsd). This version compiles for Java 6.

Only version 3.x of the Metrics library is supported.

Note: The previous version of this library was published under 'com.bol.codahale.metrics:metrics-statsd' with java package 'com.bol.codahale.metrics'. 
From now on this library is published under 'com.bol.codahale.metrics:metrics-statsd' and java package 'com.bol.dropwizard.metrics'. 
So when switching to this version you not only need to change the maven dependency but also fix some packaging.

## Quick Start

The 3.x version of the Metrics library uses the builder pattern to construct reporters. Below is an example of how to
create a StatsdReporter and report out metrics every 15 seconds.

 ```java
 import com.bol.dropwizard.metrics.reporting.StatsdReporter
 import com.bol.dropwizard.metrics.reporting.statsd.StatsD
 import com.codahale.metrics.MetricFilter
 import java.util.concurrent.TimeUnit
 
 final Statsd statsd = new Statsd("localhost", port);

 StatsdReporter reporter StatsdReporter.forRegistry(registry)
         .prefixedWith("foo")
         .withTags("My", "tag")
         .convertDurationsTo(TimeUnit.MILLISECONDS)
         .convertRatesTo(TimeUnit.SECONDS)
         .filter(MetricFilter.ALL)
         .build(statsd);
 reporter.start(15, TimeUnit.SECONDS);
```

# Setting Up Maven
## Maven Repositories

Not in a public repository yet.

## Dependency

```xml
<dependencies>
    <dependency>
        <groupId>com.bol.dropwizard.metrics</groupId>
        <artifactId>metrics-statsd</artifactId>
        <version>${metrics-statsd.version}</version>
    </dependency>
</dependencies>
```

# Release 

## 1.2.0

 * Added support for reporting boolean gauges. True is reported as 1 and False is reported as 0. 
  
## 1.1.0

 * Upgraded to Dropwizard Metrics 3.1.2
 * Changed package to 'com.bol.dropwizard.metrics.reporting'
 * Changed groupId to 'com.bol.dropwizard.metrics