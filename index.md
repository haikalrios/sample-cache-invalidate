# Application Cache with Distributed Invalidation using Quarkus, Caffeine, and Apache Kafka

This project demonstrates how to implement a local cache in a Quarkus application using Caffeine as the cache provider, and how to invalidate this cache across multiple instances of the application using messages via Apache Kafka. The goal is to maintain data consistency in distributed environments without the complexity of a fully distributed cache.

## Table of Contents

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Configuration](#project-configuration)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Running the Application](#running-the-application)
- [Testing Cache Invalidation](#testing-cache-invalidation)
- [Final Considerations](#final-considerations)
- [References](#references)

## Introduction

Modern applications often use caching to improve performance and reduce response latency. In distributed environments, maintaining cache consistency across multiple instances can be challenging. This project presents a solution that combines local caching with distributed invalidation, maintaining the efficiency of local caching while ensuring data consistency.

## Architecture

- **Quarkus**: A cloud-native Java framework for building efficient applications.
- **Caffeine Cache**: A high-performance caching library for Java.
- **Apache Kafka**: A distributed streaming platform for message exchange.
- **Quarkus Reactive Messaging**: Enables reactive communication between components using messages.

## Prerequisites

- **Java 21**
- **Apache Kafka**: Running instance (local or remote)
- **Gradle**: To build the project
- **Quarkus CLI (optional)**: To facilitate development

## Project Configuration

### Dependencies in `build.gradle`

The main dependencies include:

```gradle
dependencies {
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'io.quarkus:quarkus-smallrye-metrics'
    implementation 'io.quarkus:quarkus-arc'
    implementation 'io.quarkus:quarkus-smallrye-reactive-messaging'
    implementation 'io.quarkus:quarkus-smallrye-reactive-messaging-kafka'
    implementation 'io.quarkus:quarkus-rest'
    implementation 'io.quarkus:quarkus-cache'
    testImplementation 'io.quarkus:quarkus-junit5'
}
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── br/com/haikal/cache/
│   │       ├── DataService.java
│   │       └── DataEntrypoint.java
│   └── resources/
│       └── application.properties
```

- **DataService.java**: Simulates a service applying caching.
- **DataEntrypoint.java**: Exposes REST endpoints to interact with the application.
- **application.properties**: Application configurations, including cache and Kafka settings.

## How It Works

### 1. Local Cache with Caffeine

The `getData(String key)` method is annotated with `@CacheResult`, indicating that the results should be stored in the cache.

```java
@CacheResult(cacheName = MY_CACHE_DATA)
public String getData(String key) {
    LOGGER.info("Data retrieved from the primary source. [key:{}]", key);
    // Logic to obtain data from the primary source
    return "Sample data for key " + key;
}
```

### 2. Cache Invalidation

When data is updated, the `updateData(String key)` method is called, which sends an invalidation message to other instances via Kafka.

```java
public void updateData(String key) {
    LOGGER.info("Data update invoked. [key:{}]", key);
    // Logic to update data in the primary source

    // Sends the key to invalidate the cache in other instances
    LOGGER.info("Cache invalidation notification sent to other instances. [key:{}]", key);
    emitter.send(key);
}
```

### 3. Receiving Invalidation Messages

The `onCacheInvalidation(String key)` method receives invalidation messages and calls `invalidateData(String key)` to invalidate the local cache.

```java
@Incoming("cache-invalidation-in")
protected void onCacheInvalidation(String key) {
    LOGGER.info("Cache invalidation request received for key {}", key);
    invalidateData(key);
}

@CacheInvalidate(cacheName = MY_CACHE_DATA)
protected void invalidateData(String key) {
    LOGGER.info("Cache invalidation invoked. [key:{}]", key);
}
```

## Running the Application

### 1. Start Apache Kafka

Ensure that Apache Kafka is running and accessible by the application. You can use a Docker container to run Kafka locally.

### 2. Configure `application.properties`

Essential configurations include:

```properties
# Outgoing channel configurations (sending messages)
mp.messaging.outgoing.cache-invalidation-out.connector=smallrye-kafka
mp.messaging.outgoing.cache-invalidation-out.topic=cache-invalidation
mp.messaging.outgoing.cache-invalidation-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.cache-invalidation-out.bootstrap.servers=localhost:9092

# Incoming channel configurations (receiving messages)
mp.messaging.incoming.cache-invalidation-in.connector=smallrye-kafka
mp.messaging.incoming.cache-invalidation-in.topic=cache-invalidation
mp.messaging.incoming.cache-invalidation-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
mp.messaging.incoming.cache-invalidation-in.auto.offset.reset=latest
mp.messaging.incoming.cache-invalidation-in.broadcast=true
mp.messaging.incoming.cache-invalidation-in.bootstrap.servers=localhost:9092

# Logging configurations
quarkus.log.level=ERROR
quarkus.log.category."br.com.haikal".level=INFO
quarkus.log.category."io.quarkus.cache.runtime.CacheResultInterceptor".level=DEBUG

# Enable cache statistics
quarkus.cache.caffeine."my-cache-data".stats-enabled=true
```

### 3. Build and Run the Application

Use Gradle to build and run the application:

```bash
./gradlew build
./gradlew quarkusDev
```

### 4. Scale the Application (Optional)

To simulate multiple instances, you can start the application on different ports or machines. Ensure that all instances point to the same Kafka cluster.

## Testing Cache Invalidation

### 1. Retrieve Data via REST API

Make a GET request to retrieve data:

```bash
curl http://localhost:8080/data/get/myKey
```

**Response:**

```
Sample data for key myKey
```

Check the logs to see that the data was obtained from the primary source and cached.

### 2. Retrieve Data Again

Repeat the request:

```bash
curl http://localhost:8080/data/get/myKey
```

This time, the data will be retrieved from the cache, and the primary source will not be consulted.

### 3. Update the Data

Make a request to update the data:

```bash
curl http://localhost:8080/data/update/myKey
```

This simulates an update to the data in the primary source and sends an invalidation message.

### 4. Verify Cache Invalidation

After the update, make the request to retrieve the data again:

```bash
curl http://localhost:8080/data/get/myKey
```

The data will be obtained from the primary source again, indicating that the cache was invalidated.

### 5. Check Other Instances

If there are other instances running, they will also have invalidated their local caches for the key `myKey`.

## Final Considerations

This project demonstrates an efficient approach to maintaining cache consistency in distributed applications:

- **Local Cache**: Offers superior performance due to fast access to local memory.
- **Distributed Invalidation**: Ensures all instances keep data updated without the need for a centralized cache.
- **Simplicity**: Avoids the additional complexity of a fully distributed cache.

### Possible Extensions

- **Exception Handling**: Implement error handling in communication with Kafka.
- **Security**: Add authentication and encryption to communications.
- **Monitoring**: Integrate monitoring tools to track cache and messaging performance.

## References

- [Quarkus - Cache Guide](https://quarkus.io/guides/cache)
- [Quarkus - Reactive Messaging with Kafka](https://quarkus.io/guides/kafka)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Apache Kafka](https://kafka.apache.org/)

---

**Note:** This project is for educational purposes and can be adapted according to the specific needs of your application.

---