# RedFox TestKit

A testing utility, containing a common setup for integration tests, such as Testcontainers configuration and shared test
utilities.

Documentation is available directly in-code as `KDoc` comments.

## Brief Overview

### **`KafkaAwareTest`** Interface

An interface fixture to add on test classes to enable Kafka Testcontainers setup.

```kotlin
class MyKafkaTest : KafkaAwareTest {
    // Test code here
}
```

### **`PostgresAwareTest`** Interface

An interface fixture to add on test classes to enable PostgreSQL Testcontainers setup.

```kotlin
class MyPostgresTest : PostgresAwareTest {
    // Test code here
}
```

### **`@TestListener`** Annotation

Annotation to add on `TestKafkaConsumer` for automatic registration of the listener in tests.

```kotlin
class MyKafkaTest : KafkaAwareTest {

    @TestListener($$"${my.topic}")
    private lateinit var consumer: TestKafkaConsumer

    // Test code here
}
```
