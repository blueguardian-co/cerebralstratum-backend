# utils

Shared library JAR providing common types and utilities consumed by other modules in this project.

**Note:** This module is a library — it is not a runnable service and should not be deployed independently.

## Contents

- `co.blueguardian.cerebralstratum.utils.messaging` — Shared Kafka message DTOs (`LocationMessage`, `StatusMessage`) and their Jackson deserializers
- `co.blueguardian.cerebralstratum.utils.uuid` — `UUIDv5Generator` utility

## Usage

Add the dependency to your module's `pom.xml`:

```xml
<dependency>
    <groupId>co.blueguardian.cerebralstratum</groupId>
    <artifactId>utils</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Building

```shell script
./mvnw install -pl utils -am
```

Or from the repository root:

```shell script
make build
```
