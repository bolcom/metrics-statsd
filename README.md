# metrics-statsd

Statsd reporter for [codahale/metrics] (https://github.com/codahale/metrics), based on Sean Laurent's [metrics-statsd] (https://github.com/organicveggie/metrics-statsd),  Mike Keesey's [metrics-statsd] (https://github.com/ReadyTalk/metrics-statsd) and  Hannes Heijkenskjöld's [metrics-statsd] (https://github.com/jjagged/metrics-statsd). This version compiles for Java 6.

Only version 3.x of the Metrics library is supported.

## Quick Start

The 3.x version of the Metrics library uses the builder pattern to construct reporters. Below is an example of how to
create a StatsdReporter and report out metrics every 15 seconds.

 ```java
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

Not in a public repository.

## Dependency

```xml
<dependencies>
    <dependency>
        <groupId>com.bol.codahale.metrics</groupId>
        <artifactId>metrics-statsd</artifactId>
        <version>${metrics-statsd.version}</version>
    </dependency>
</dependencies>
```

