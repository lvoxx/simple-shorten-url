### Database Table Creation for Transactional Example

Source: https://docs.spring.io/spring-kafka/reference/tips

SQL statement to create a simple table named 'mytable' with a single 'data' column of type varchar(20). This table is used in the transactional examples to store data inserted during the database operations.

```sql
create table mytable (data varchar(20));

```

--------------------------------

### Chaining DB and Kafka Transactions (Spring Boot Application)

Source: https://docs.spring.io/spring-kafka/reference/tips

This Spring Boot application demonstrates chaining Kafka and database transactions. The listener starts the Kafka transaction, and the @Transactional annotation starts the DB transaction. The DB transaction commits first, ensuring idempotency if the Kafka transaction redelivers.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner runner(KafkaTemplate<String, String> template) {
        return args -> template.executeInTransaction(t -> t.send("topic1", "test"));
    }

    @Bean
    public DataSourceTransactionManager dstm(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Component
    public static class Listener {

        private final JdbcTemplate jdbcTemplate;

        private final KafkaTemplate<String, String> kafkaTemplate;

        public Listener(JdbcTemplate jdbcTemplate, KafkaTemplate<String, String> kafkaTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            this.kafkaTemplate = kafkaTemplate;
        }

        @KafkaListener(id = "group1", topics = "topic1")
        @Transactional("dstm")
        public void listen1(String in) {
            this.kafkaTemplate.send("topic2", in.toUpperCase());
            this.jdbcTemplate.execute("insert into mytable (data) values ('" + in + "')");
        }

        @KafkaListener(id = "group2", topics = "topic2")
        public void listen2(String in) {
            System.out.println(in);
        }

    }

    @Bean
    public NewTopic topic1() {
        return TopicBuilder.name("topic1").build();
    }

    @Bean
    public NewTopic topic2() {
        return TopicBuilder.name("topic2").build();
    }

}

```

--------------------------------

### Kafka Producer and Listener with Java Config (Spring Kafka)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This snippet demonstrates a complete Spring Kafka application setup using Java configuration, without Spring Boot. It includes a Kafka sender class, a Kafka listener class, and a configuration class to define beans for Kafka components like `KafkaTemplate`, `ProducerFactory`, `ConsumerFactory`, and `ConcurrentKafkaListenerContainerFactory`. This setup is crucial for applications needing Kafka integration without the auto-configuration provided by Spring Boot.

```java
public class Sender {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        context.getBean(Sender.class).send("test", 42);
    }

    private final KafkaTemplate<Integer, String> template;

    public Sender(KafkaTemplate<Integer, String> template) {
        this.template = template;
    }

    public void send(String toSend, int key) {
        this.template.send("topic1", key, toSend);
    }

}

public class Listener {

    @KafkaListener(id = "listen1", topics = "topic1")
    public void listen1(String in) {
        System.out.println(in);
    }

}

@Configuration
@EnableKafka
public class Config {

    @Bean
    ConcurrentKafkaListenerContainerFactory<Integer, String>
                        kafkaListenerContainerFactory(ConsumerFactory<Integer, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Integer, String> factory =
                                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<Integer, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProps());
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // ...
        return props;
    }

    @Bean
    public Sender sender(KafkaTemplate<Integer, String> template) {
        return new Sender(template);
    }

    @Bean
    public Listener listener() {
        return new Listener();
    }

    @Bean
    public ProducerFactory<Integer, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(senderProps());
    }

    private Map<String, Object> senderProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //...
        return props;
    }

    @Bean
    public KafkaTemplate<Integer, String> kafkaTemplate(ProducerFactory<Integer, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
```

--------------------------------

### Apache Kafka Streams Support Example

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Demonstrates basic integration with Apache Kafka Streams within a Spring application. This typically involves defining Kafka Streams processors and topology.

```java
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        KStream<String, String> source = streamsBuilder.stream("input-topic");
        KStream<String, String> processedStream = source.mapValues(value -> value.toUpperCase());
        processedStream.to("output-topic");
        return source; // Or return the processedStream depending on your needs
    }
}
```

--------------------------------

### Example Usage of KafkaTestUtils for Record Consumption (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

An example demonstrating the practical application of KafkaTestUtils.getSingleRecord to retrieve and assert a single consumed record from a Kafka topic after sending a message.

```java
... 
template.sendDefault(0, 2, "bar");
ConsumerRecord<Integer, String> received = KafkaTestUtils.getSingleRecord(consumer, "topic");
...
```

--------------------------------

### Configure KafkaListener with Sequence Startup

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Demonstrates how to configure `@KafkaListener` annotations to ensure that listeners start up in a specific sequence. This is useful for managing dependencies between different message processing tasks.

```java
@Configuration
public class KafkaListenerConfig {

    @Bean
    public NewTopic topic1() {
        return TopicBuilder.name("my-topic-1").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic topic2() {
        return TopicBuilder.name("my-topic-2").partitions(1).replicas(1).build();
    }

    @KafkaListener(topics = "my-topic-1", id = "listener1", autoStartup = "false")
    public void listen1(String message) {
        System.out.println("Received message from topic 1: " + message);
    }

    @KafkaListener(topics = "my-topic-2", id = "listener2", autoStartup = "false")
    public void listen2(String message) {
        System.out.println("Received message from topic 2: " + message);
    }

    @Bean
    public ApplicationRunner runner(KafkaListenerEndpointRegistry registry) {
        return args -> {
            System.out.println("Starting listener 1...");
            registry.getListenerContainer("listener1").start();
            System.out.println("Listener 1 started.");

            System.out.println("Starting listener 2...");
            registry.getListenerContainer("listener2").start();
            System.out.println("Listener 2 started.");
        };
    }
}
```

--------------------------------

### Configure Topic and Partition Initial Offset

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Explains how to configure the initial offset for topic partitions when a Kafka consumer starts. This allows control over whether the consumer starts reading from the earliest available message, the latest, or a specific timestamp.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.IsolationLevel;

@Configuration
public class OffsetConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        factory.getContainerProperties().setListenerInitialOffset("earliest"); // or "latest", "none"
        
        // Example: Setting specific offsets for specific partitions
        factory.getContainerProperties().setPartitionOffset(
            new TopicPartition("my-specific-topic", 0), 
            "100", // offset value as String
            true // commit if false
        );
        
        return factory;
    }
}
```

--------------------------------

### Application Properties for Kafka and DB Transactions

Source: https://docs.spring.io/spring-kafka/reference/tips

Configuration properties for setting up MySQL datasource and Kafka consumer/producer settings. Notably, it enables read_committed isolation level for Kafka consumers and sets a transaction ID prefix for producers.

```properties
spring.datasource.url=jdbc:mysql://localhost/integration?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.consumer.properties.isolation.level=read_committed

spring.kafka.producer.transaction-id-prefix=tx-

#logging.level.org.springframework.transaction=trace
#logging.level.org.springframework.kafka.transaction=debug
#logging.level.org.springframework.jdbc=debug

```

--------------------------------

### Manage a Single Embedded Kafka Broker Instance - Java

Source: https://docs.spring.io/spring-kafka/reference/testing

This Java class, `EmbeddedKafkaHolder`, manages a single instance of `EmbeddedKafkaBroker` for reuse across multiple test classes. It ensures the broker is started only once and provides a static method to retrieve the broker instance. It is designed for Spring Boot environments where it can replace the `spring.kafka.bootstrap-servers` property. The broker is started using `afterPropertiesSet()` and can be explicitly destroyed if needed.

```java
public final class EmbeddedKafkaHolder {

    private static EmbeddedKafkaBroker embeddedKafka = new EmbeddedKafkaZKBroker(1, false)
            .brokerListProperty("spring.kafka.bootstrap-servers");

    private static boolean started;

    public static EmbeddedKafkaBroker getEmbeddedKafka() {
        if (!started) {
            synchronized (this) {
		if (!started) {
		    try {
	                embeddedKafka.afterPropertiesSet();
		    }
	            catch (Exception e) {
	                throw new KafkaException("Embedded broker failed to start", e);
		    }
	            started = true;
		}
	    }
        }
        return embeddedKafka;
    }
}
```

--------------------------------

### Producer-Only Transaction Synchronization (KafkaTemplate)

Source: https://docs.spring.io/spring-kafka/reference/tips

Demonstrates producer-only transaction synchronization with a database transaction manager ('dstm'). The KafkaTemplate will align its transaction with the DB transaction, ensuring both commit or rollback together.

```java
@Transactional("dstm")
public void someMethod(String in) {
    this.kafkaTemplate.send("topic2", in.toUpperCase());
    this.jdbcTemplate.execute("insert into mytable (data) values ('" + in + "')");
}

```

--------------------------------

### Committing Kafka Transaction First (Nested @Transactional)

Source: https://docs.spring.io/spring-kafka/reference/tips

Example using nested @Transactional methods to commit the Kafka transaction before the database transaction. This approach ensures the DB transaction only commits if the Kafka transaction is successful.

```java
@Transactional("dstm")
public void someMethod(String in) {
    this.jdbcTemplate.execute("insert into mytable (data) values ('" + in + "')");
    sendToKafka(in);
}

@Transactional("kafkaTransactionManager")
public void sendToKafka(String in) {
    this.kafkaTemplate.send("topic2", in.toUpperCase());
}

```

--------------------------------

### Configure Sequential Kafka Listener Startup with Java

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

This Java snippet demonstrates configuring multiple Kafka listeners with different `containerGroup` IDs. It also shows how to create a `ContainerGroupSequencer` bean to manage the startup order of these groups, ensuring listeners in 'g1' complete before 'g2' starts. The sequencer controls auto-startup and idle event intervals.

```java
@KafkaListener(id = "listen1", topics = "topic1", containerGroup = "g1", concurrency = "2")
public void listen1(String in) {
}

@KafkaListener(id = "listen2", topics = "topic2", containerGroup = "g1", concurrency = "2")
public void listen2(String in) {
}

@KafkaListener(id = "listen3", topics = "topic3", containerGroup = "g2", concurrency = "2")
public void listen3(String in) {
}

@KafkaListener(id = "listen4", topics = "topic4", containerGroup = "g2", concurrency = "2")
public void listen4(String in) {
}

@Bean
ContainerGroupSequencer sequencer(KafkaListenerEndpointRegistry registry) {
    return new ContainerGroupSequencer(registry, 5000, "g1", "g2");
}
```

--------------------------------

### Mock Producer Factory Configuration for Spring Kafka (Non-Transactional)

Source: https://docs.spring.io/spring-kafka/reference/testing

Configures a MockProducerFactory for creating non-transactional MockProducers. This is useful for testing scenarios where transactions are not required, simplifying producer setup.

```java
@Bean
ProducerFactory<String, String> nonTransFactory() {
    return new MockProducerFactory<>(() ->
            new MockProducer<>(true, new StringSerializer(), new StringSerializer()));
}
```

--------------------------------

### Spring Boot Application Setup for Kafka Interceptors

Source: https://docs.spring.io/spring-kafka/reference/kafka/interceptors

This Java code sets up a Spring Boot application to configure Kafka producer and consumer factories. It demonstrates how to include custom interceptors and inject a 'SomeBean' dependency into them via configuration properties. This is crucial for enabling Spring Bean injection into interceptors managed by Kafka.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ConsumerFactory<?, ?> kafkaConsumerFactory(SomeBean someBean) {
        Map<String, Object> consumerProperties = new HashMap<>();
        // consumerProperties.put(..., ...)
        // ...
        consumerProperties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, MyConsumerInterceptor.class.getName());
        consumerProperties.put("some.bean", someBean);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    public ProducerFactory<?, ?> kafkaProducerFactory(SomeBean someBean) {
        Map<String, Object> producerProperties = new HashMap<>();
        // producerProperties.put(..., ...)
        // ...
        producerProperties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, MyProducerInterceptor.class.getName());
        producerProperties.put("some.bean", someBean);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    public SomeBean someBean() {
        return new SomeBean();
    }

    @KafkaListener(id = "kgk897", topics = "kgh897")
    public void listen(String in) {
        System.out.println("Received " + in);
    }

    @Bean
    public ApplicationRunner runner(KafkaTemplate<String, String> template) {
        return args -> template.send("kgh897", "test");
    }

    @Bean
    public NewTopic kRequests() {
        return TopicBuilder.name("kgh897")
            .partitions(1)
            .replicas(1)
            .build();
    }

}

```

--------------------------------

### Embedded Broker with @EmbeddedKafka Annotation

Source: https://docs.spring.io/spring-kafka/reference/testing

This example shows how to use the @EmbeddedKafka annotation to create an embedded Kafka broker for testing. It configures the topics and the bootstrap server property, with the latter being the default since version 3.0.10.

```java
@RunWith(SpringRunner.class)
@EmbeddedKafka(topics = "someTopic",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers") // this is now the default
public class MyApplicationTests {

    @Autowired
    private KafkaTemplate<String, String> template;

    @Test
    public void test() {
        ...
    }

}
```

--------------------------------

### Apache Kafka Streams Integration

Source: https://docs.spring.io/spring-kafka/reference/search

Provides an example of integrating Apache Kafka Streams with Spring for Kafka. This allows building stream processing applications using Spring's programming model, leveraging Kafka's powerful stream processing capabilities.

```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG)
    public StreamsConfig kStreamsConfigs() {
        Map<String, String> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // Add other Kafka Streams configurations as needed
        return new StreamsConfig(props);
    }

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream("input-topic");
        stream.mapValues(value -> value.toUpperCase())
              .to("output-topic");
        return stream;
    }
}
```

--------------------------------

### Implementing ApplicationListener for ConsumerStoppedEvent

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/thread-safety

This example shows how to implement the `ApplicationListener` interface to receive and process `ConsumerStoppedEvent`. This approach can be used as an alternative to `@EventListener` for cleaning up thread-specific resources.

```java
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class ConsumerStoppedApplicationListener implements ApplicationListener<ConsumerStoppedEvent> {

    @Override
    public void onApplicationEvent(ConsumerStoppedEvent event) {
        // Clean up ThreadLocal instances or remove thread-scoped beans
        System.out.println("Consumer stopped event received, cleaning up for group: " + event.getGroupId());
    }
}
```

--------------------------------

### Configure MessagingMessageConverter with Header Mapper (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Provides a Java Bean configuration example for creating a MessagingMessageConverter and setting a DefaultKafkaHeaderMapper with string encoding enabled. This converter can then be used with KafkaTemplate.

```java
@Bean
MessagingMessageConverter converter() {
    MessagingMessageConverter converter = new MessagingMessageConverter();
    DefaultKafkaHeaderMapper mapper = new DefaultKafkaHeaderMapper();
    mapper.setEncodeStrings(true);
    converter.setHeaderMapper(mapper);
    return converter;
}
```

--------------------------------

### Asynchronous @KafkaListener Return Types

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Explains and provides an example of using asynchronous return types with the `@KafkaListener` annotation, often used with CompletableFuture for non-blocking operations.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncListener {

    @KafkaListener(topics = "asyncTopic")
    public CompletableFuture<String> processAsync(String message) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate asynchronous processing
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Processed: " + message;
        });
    }
}
```

--------------------------------

### Configuring KafkaListenerContainerFactory with Custom Reply Headers

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This example shows how to configure a ConcurrentKafkaListenerContainerFactory with a custom ReplyHeadersConfigurer. It demonstrates overriding the shouldCopy method and providing additional headers to be sent with reply messages. A KafkaTemplate is also set.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<Integer, String> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<Integer, String> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf());
    factory.setReplyTemplate(template());
    factory.setReplyHeadersConfigurer(new ReplyHeadersConfigurer() {

      @Override
      public boolean shouldCopy(String headerName, Object headerValue) {
        return false;
      }

      @Override
      public Map<String, Object> additionalHeaders() {
        return Collections.singletonMap("qux", "fiz");
      }

    });
    return factory;
}
```

--------------------------------

### Dynamically Assign All Partitions to KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/tips

This snippet demonstrates how to use a SpEL expression within the `@KafkaListener` annotation to dynamically determine and assign all partitions for a given topic. It includes a custom `PartitionFinder` bean that utilizes a `ConsumerFactory` to fetch partition information at application startup. This approach is useful for scenarios like loading compacted topics into a cache, where Kafka's group management is bypassed. The `AckMode` should be set to `MANUAL` to avoid offset commits when no consumer group ID is specified.

```java
@KafkaListener(topicPartitions = @TopicPartition(topic = "compacted",
            partitions = "#{@finder.partitions('compacted')}",
            partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0")))
public void listen(@Header(KafkaHeaders.RECEIVED_KEY) String key, String payload) {
    // ...
}

@Bean
public PartitionFinder finder(ConsumerFactory<String, String> consumerFactory) {
    return new PartitionFinder(consumerFactory);
}

public static class PartitionFinder {

    private final ConsumerFactory<String, String> consumerFactory;

    public PartitionFinder(ConsumerFactory<String, String> consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    public String[] partitions(String topic) {
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            return consumer.partitionsFor(topic).stream()
                .map(pi -> "" + pi.partition())
                .toArray(String[]::new);
        }
    }

}
```

--------------------------------

### Retrieve a Queryable Store by Name and Type

Source: https://docs.spring.io/spring-kafka/reference/streams

This example shows how to retrieve a queryable state store from the `KafkaStreamsInteractiveQueryService`. It uses the store's name ('app-store') and specifies the type as `keyValueStore` using `QueryableStoreTypes.keyValueStore()`.

```java
@Autowired
private KafkaStreamsInteractiveQueryService interactiveQueryService;

ReadOnlyKeyValueStore<Object, Object>  appStore = interactiveQueryService.retrieveQueryableStore("app-store", QueryableStoreTypes.keyValueStore());
```

--------------------------------

### Configure Custom RetryTopicConfiguration Beans

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

This example demonstrates configuring multiple RetryTopicConfiguration beans with specific retry policies for different topics. It allows for fine-grained control over backoff strategies, maximum attempts, concurrency, and which exceptions trigger retries, as well as including or excluding specific topics.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .fixedBackOff(3000)
            .maxAttempts(5)
            .concurrency(1)
            .includeTopics("my-topic", "my-other-topic")
            .create(template);
}

@Bean
public RetryTopicConfiguration myOtherRetryTopic(KafkaTemplate<String, MyOtherPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .exponentialBackoff(1000, 2, 5000)
            .maxAttempts(4)
            .excludeTopics("my-topic", "my-other-topic")
            .retryOn(MyException.class)
            .create(template);
```

--------------------------------

### Multi-Listener Class with @SendTo Annotations

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This example shows a class with multiple KafkaListeners. One listener forwards to a static topic, while another uses a runtime SpEL expression to determine the reply topic based on headers. A KafkaTemplate is required for each.

```java
@KafkaListener(topics = "annotated25")
@SendTo("annotated25reply1")
public class MultiListenerSendTo {

    @KafkaHandler
    public String foo(String in) {
        ...
    }

    @KafkaHandler
    @SendTo("!{'annotated25reply2'}")
    public String bar(@Payload(required = false) KafkaNull nul,
            @Header(KafkaHeaders.RECEIVED_KEY) int key) {
        ...
    }

}
```

--------------------------------

### KafkaListener with @SendTo using Runtime SpEL Expression

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This example demonstrates a KafkaListener that sends its return value to a topic determined by a runtime SpEL expression evaluating the request's value. It requires a KafkaTemplate to be configured in the listener container factory.

```java
@KafkaListener(topics = "annotated21")
@SendTo("!{request.value()}") // runtime SpEL
public String replyingListener(String in) {
    ...
}
```

--------------------------------

### Using KafkaTemplate to Receive

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Demonstrates a less common but supported pattern where KafkaTemplate can be used to receive messages, typically in scenarios involving request-reply or specific testing setups. This leverages the template's ability to send and receive within a unified interface.

```java
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

public class KafkaTemplateReceiveExample {

    private final KafkaTemplate<String, String> kafkaTemplate;
    // Or ReplyingKafkaTemplate for request-reply scenarios
    // private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;

    public KafkaTemplateReceiveExample(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        // this.replyingKafkaTemplate = replyingKafkaTemplate;
    }

    public void sendAndReceive(String requestTopic, String replyTopic, String request) {
        // This is a simplified example. For actual request-reply, ReplyingKafkaTemplate is preferred.
        // This demonstrates sending and then potentially setting up a listener to receive.
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(requestTopic, request);
        
        future.addCallback(new org.springframework.util.concurrent.ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                System.out.println("Sent message successfully: " + request);
                // In a real scenario, you'd have a listener for the replyTopic
                // or use ReplyingKafkaTemplate which handles this internally.
            }

            @Override
            public void onFailure(Throwable ex) {
                System.err.println("Failed to send message: " + request + ", error: " + ex.getMessage());
            }
        });
    }
}
```

--------------------------------

### Non-Blocking Retries Configuration in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Provides an example of configuring non-blocking retries for message processing in Spring Kafka. This pattern is useful for handling transient failures without blocking the consumer thread.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        // Non-blocking retries with a fixed back-off
        SeekToCurrentErrorHandler errorHandler = new SeekToCurrentErrorHandler(
            new FixedBackOff(1000L, 5) // Delay of 1 second, 5 retries
        );
        return errorHandler;
    }

    // Ensure your KafkaListenerContainerFactory is configured to use this errorHandler
    // factory.setErrorHandler(errorHandler()); 
}
```

--------------------------------

### Define StreamsBuilderFactoryBean for Kafka Streams

Source: https://docs.spring.io/spring-kafka/reference/streams

Defines a StreamsBuilderFactoryBean as a Spring bean to manage Kafka Streams lifecycle and expose a StreamsBuilder singleton. It takes KafkaStreamsConfiguration as input. Starting with version 2.2, KafkaStreamsConfiguration is preferred over StreamsConfig.

```java
@Bean
public FactoryBean<StreamsBuilder> myKStreamBuilder(KafkaStreamsConfiguration streamsConfig) {
    return new StreamsBuilderFactoryBean(streamsConfig);
}
```

--------------------------------

### Disable DLT Container Auto-Startup via Builder

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/dlt-strategies

This example demonstrates disabling the DLT container's auto-startup using the `RetryTopicConfigurationBuilder` by calling `autoStartDltHandler(false)`. This provides programmatic control over the DLT container's initiation.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .autoStartDltHandler(false)
            .create(template);
}
```

--------------------------------

### Enable Global Embedded Kafka Listener - Properties

Source: https://docs.spring.io/spring-kafka/reference/testing

This configuration snippet shows how to enable the `GlobalEmbeddedKafkaTestExecutionListener` for JUnit Platform by setting the `spring.kafka.global.embedded.enabled` property to `true`. This listener starts a single global `EmbeddedKafkaBroker` for the entire test plan. Additional properties like count, ports, topics, and partitions can be configured to customize the broker's behavior.

```properties
spring.kafka.global.embedded.enabled=true
spring.kafka.embedded.count=1
spring.kafka.embedded.ports=0
spring.kafka.embedded.topics=topic1,topic2
spring.kafka.embedded.partitions=1
spring.kafka.embedded.broker.properties.location=classpath:kafka-broker.properties
spring.kafka.embedded.kraft=true
```

--------------------------------

### Spring Kafka: Test KafkaTemplate with Embedded Kafka (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

This Java code demonstrates how to test Spring Kafka's `KafkaTemplate` using an `EmbeddedKafkaRule`. It sets up an embedded Kafka broker, configures a Kafka listener container to consume messages, and then uses `KafkaTemplate` to send messages. Assertions verify that the correct messages are received. This example relies on Spring Kafka and JUnit.

```java
public class KafkaTemplateTests {

    private static final String TEMPLATE_TOPIC = "templateTopic";

    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, true, TEMPLATE_TOPIC);

    @Test
    public void testTemplate() throws Exception {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false",
            embeddedKafka.getEmbeddedKafka());
        DefaultKafkaConsumerFactory<Integer, String> cf = 
                            new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(TEMPLATE_TOPIC);
        KafkaMessageListenerContainer<Integer, String> container = 
                            new KafkaMessageListenerContainer<>(cf, containerProperties);
        final BlockingQueue<ConsumerRecord<Integer, String>> records = new LinkedBlockingQueue<>();
        container.setupMessageListener(new MessageListener<Integer, String>() {

            @Override
            public void onMessage(ConsumerRecord<Integer, String> record) {
                System.out.println(record);
                records.add(record);
            }

        });
        container.setBeanName("templateTests");
        container.start();
        ContainerTestUtils.waitForAssignment(container,
                            embeddedKafka.getEmbeddedKafka().getPartitionsPerTopic());
        Map<String, Object> producerProps =
                            KafkaTestUtils.producerProps(embeddedKafka.getEmbeddedKafka());
        ProducerFactory<Integer, String> pf =
                            new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
        template.setDefaultTopic(TEMPLATE_TOPIC);
        template.sendDefault("foo");
        assertThat(records.poll(10, TimeUnit.SECONDS), hasValue("foo"));
        template.sendDefault(0, 2, "bar");
        ConsumerRecord<Integer, String> received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received, hasKey(2));
        assertThat(received, hasPartition(0));
        assertThat(received, hasValue("bar"));
        template.send(TEMPLATE_TOPIC, 0, 2, "baz");
        received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received, hasKey(2));
        assertThat(received, hasPartition(0));
        assertThat(received, hasValue("baz"));
    }

}
```

--------------------------------

### Configure Multi-Handler KafkaListener for Null Payloads (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/tombstones

This example shows how to configure a class-level `@KafkaListener` with multiple `@KafkaHandler` methods to manage different payload types, including null payloads using `KafkaNull`. A dedicated handler is set up to process `KafkaNull` for key deletion.

```java
@KafkaListener(id = "multi", topics = "myTopic")
static class MultiListenerBean {

    @KafkaHandler
    public void listen(String cat) {
        ...
    }

    @KafkaHandler
    public void listen(Integer hat) {
        ...
    }

    @KafkaHandler
    public void delete(@Payload(required = false) KafkaNull nul, @Header(KafkaHeaders.RECEIVED_KEY) int key) {
        ...
    }

}
```

--------------------------------

### Disable DLT Container Auto-Startup

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/dlt-strategies

This snippet illustrates how to prevent the DLT container from starting automatically when using the `@RetryableTopic` annotation by setting `autoStartDltHandler` to `false`. This allows for manual control over the DLT handler's lifecycle.

```java
@RetryableTopic(autoStartDltHandler = false)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Access Kafka Producer Metrics using Micrometer

Source: https://docs.spring.io/spring-kafka/reference/kafka/micrometer

This example shows how to retrieve and count a specific Kafka producer metric ('kafka.producer.node.incoming.byte.total') using Micrometer's `meterRegistry`. It demonstrates querying metrics by specific tags, including a custom tag and the automatically added 'spring.id' tag, which is derived from the producer factory and client ID.

```java
double count = this.meterRegistry.get("kafka.producer.node.incoming.byte.total")
                .tag("customTag", "customTagValue")
                .tag("spring.id", "myProducerFactory.myClientId-1")
                .functionCounter()
                .count();
```

--------------------------------

### Exclude Test Support Binder in @SpringBootTest

Source: https://docs.spring.io/spring-kafka/reference/testing

This example demonstrates how to exclude the spring-cloud-stream test support binder when using an embedded broker for tests in a Spring Boot application. This is necessary to ensure the real binder is used instead of the test binder.

```java
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.autoconfigure.exclude="
    + "org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration")
public class MyApplicationTests {
    ...
}
```

--------------------------------

### Kafka Consumer Auto Offset Reset Configuration

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

Specifies the consumer's behavior when no initial offset is found. Setting `spring.kafka.consumer.auto-offset-reset=earliest` ensures that the consumer starts reading from the beginning of the topic's log if no committed offset exists. This is a common setting for development or when reprocessing is desired.

```properties
spring.kafka.consumer.auto-offset-reset=earliest

```

--------------------------------

### Retrieve Kafka Streams Host Information (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

This code snippet demonstrates how to retrieve the host information for a Kafka Streams processor instance that is responsible for a specific key. It utilizes the KafkaStreamsInteractiveQueryService to find the instance based on the store name, key, and serializer. This is crucial for querying remote state stores in a distributed setup.

```java
@Autowired
private KafkaStreamsInteractiveQueryService interactiveQueryService;

HostInfo kafkaStreamsApplicationHostInfo = this.interactiveQueryService.getKafkaStreamsApplicationHostInfo("app-store", 12345, new IntegerSerializer());
```

--------------------------------

### KafkaTemplate Local Transaction Execution

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/transactions

This example shows how to execute a series of KafkaTemplate send operations within a local, self-contained transaction using KafkaTemplate.executeInTransaction. The transaction is automatically committed if the provided callback completes successfully, and rolled back if any exception is thrown.

```java
boolean result = template.executeInTransaction(t -> {
    t.sendDefault("thing1", "thing2");
    t.sendDefault("cat", "hat");
    return true;
});
```

--------------------------------

### Single Topic for MaxInterval Exponential Delay with RetryTopic

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

Configure RetryTopic to use a single retry topic for attempts with the `maxInterval` delay in an exponential backoff policy. This strategy helps manage a large number of retry attempts without creating an excessive number of topics. The default behavior starting from version 3.2 reuses retry topics for same intervals.

```java
@RetryableTopic(attempts = 230,
    backoff = @Backoff(delay = 1_000, multiplier = 2, maxDelay = 16_000),
    sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.MULTIPLE_TOPICS)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .exponentialBackoff(1_000, 2, 16_000)
            .maxAttempts(230)
            .useSingleTopicForSameIntervals()
            .create(template);
}
```

--------------------------------

### KafkaTestUtils Helper Methods for Testing Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/testing

Introduces the KafkaTestUtils class from 'spring-kafka-test', which offers static helper methods for common testing tasks such as consuming records and retrieving offsets. Refer to its Javadocs for detailed usage.

```java
org.springframework.kafka.test.utils.KafkaTestUtils
```

--------------------------------

### Configure Combined Blocking and Non-Blocking Retries in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/retry-topic-combine-blocking

This example shows how to configure both blocking and non-blocking retries simultaneously. It overrides `configureBlockingRetries` for blocking retries and `manageNonBlockingFatalExceptions` to specify exceptions that should bypass all retries and go directly to the Dead Letter Topic (DLT). This allows fine-grained control over retry behavior for different exception types.

```java
@Override
protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
    blockingRetries
            .retryOn(ShouldRetryOnlyBlockingException.class, ShouldRetryViaBothException.class)
            .backOff(new FixedBackOff(50, 3));
}

@Override
protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
    nonBlockingFatalExceptions.add(ShouldSkipBothRetriesException.class);
}
```

--------------------------------

### Custom JsonSerializer with Customized ObjectMapper in Java

Source: https://docs.spring.io/spring-kafka/reference/tips

This Java code snippet demonstrates how to create a custom JsonSerializer by extending the default JsonSerializer and passing a customized ObjectMapper to its constructor. The customized ObjectMapper disables WRITE_DATES_AS_TIMESTAMPS serialization feature. This is useful when you need to apply specific Jackson configurations to your JSON serialization process.

```java
public class CustomJsonSerializer extends JsonSerializer<Object> {

    public CustomJsonSerializer() {
        super(customizedObjectMapper());
    }

    private static ObjectMapper customizedObjectMapper() {
        ObjectMapper mapper = JacksonUtils.enhancedObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}
```

--------------------------------

### Spring Boot Kafka Producer App Configuration

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

Configures a Spring Boot application to produce messages to a Kafka topic. It includes a `NewTopic` bean for topic creation and an `ApplicationRunner` to send a test message upon startup. This is useful for basic message production scenarios.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("topic1")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public ApplicationRunner runner(KafkaTemplate<String, String> template) {
        return args -> {
            template.send("topic1", "test");
        };
    }

}

```

```kotlin
@SpringBootApplication
class Application {

    @Bean
    fun topic() = NewTopic("topic1", 10, 1)

    @Bean
    fun runner(template: KafkaTemplate<String?, String?>) =
        ApplicationRunner { template.send("topic1", "test") }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runApplication<Application>(*args)
    }

}

```

--------------------------------

### Spring Boot Kafka Consumer App Configuration

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

Sets up a Spring Boot application to consume messages from a Kafka topic. It defines a Kafka listener and configures topic creation. The `application.properties` file specifies the auto-offset reset policy for the consumer. This snippet is applicable if the topic does not already exist on the broker.

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("topic1")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @KafkaListener(id = "myId", topics = "topic1")
    public void listen(String in) {
        System.out.println(in);
    }

}

```

```kotlin
@SpringBootApplication
class Application {

    @Bean
    fun topic() = NewTopic("topic1", 10, 1)

    @KafkaListener(id = "myId", topics = ["topic1"])
    fun listen(value: String?) {
        println(value)
    }

}

fun main(args: Array<String>) = runApplication<Application>(*args)

```

--------------------------------

### Configuring Kafka Topics with Spring

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

This section explains how to configure Kafka topics for use with Spring. It covers defining topic names, partitions, and replicas, which are essential for managing Kafka data streams within a Spring application. No specific dependencies are required beyond the core Spring for Apache Kafka library.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic myTopic() {
        return new NewTopic("my-topic", 10, 1);
    }

    // Other configurations for KafkaTemplate if needed
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

--------------------------------

### Add Spring Kafka Dependency (Gradle)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This snippet demonstrates how to include the spring-kafka library in a Gradle project. It specifies the dependency for compilation.

```gradle
compile 'org.springframework.kafka:spring-kafka:3.1.10'
```

--------------------------------

### Add Kafka Topics Programmatically (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

Demonstrates how to add Kafka topics with specific partition counts and replication factors using `EmbeddedKafkaRule`. It shows creating topics with default configurations and dynamically adding more topics with custom settings.

```java
public class MyTests {

    @ClassRule
    private static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, false, 5, "cat", "hat");

    @Test
    public void test() {
        embeddedKafkaRule.getEmbeddedKafka()
              .addTopics(new NewTopic("thing1", 10, (short) 1), new NewTopic("thing2", 15, (short) 1));
        ...
    }

}
```

--------------------------------

### Kafka Producer and Listener with Kotlin Config (Spring Kafka)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This snippet showcases a Spring Kafka application configured using Kotlin, demonstrating the same producer and listener functionality as the Java version but with Kotlin's concise syntax. It defines the necessary beans for Kafka integration, including `KafkaTemplate`, `ProducerFactory`, `ConsumerFactory`, and `ConcurrentKafkaListenerContainerFactory`. This is suitable for Kotlin-based projects requiring Spring Kafka features without Spring Boot's auto-configuration.

```kotlin
class Sender(private val template: KafkaTemplate<Int, String>) {

    fun send(toSend: String, key: Int) {
        template.send("topic1", key, toSend)
    }

}

class Listener {

    @KafkaListener(id = "listen1", topics = ["topic1"])
    fun listen1(`in`: String) {
        println(`in`)
    }

}

@Configuration
@EnableKafka
class Config {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<Int, String>) =
        ConcurrentKafkaListenerContainerFactory<Int, String>().also { it.consumerFactory = consumerFactory }


    @Bean
    fun consumerFactory() = DefaultKafkaConsumerFactory<Int, String>(consumerProps)

    val consumerProps = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        ConsumerConfig.GROUP_ID_CONFIG to "group",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to IntegerDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
    )

    @Bean
    fun sender(template: KafkaTemplate<Int, String>) = Sender(template)

    @Bean
    fun listener() = Listener()

    @Bean
    fun producerFactory() = DefaultKafkaProducerFactory<Int, String>(senderProps)

    val senderProps = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        ProducerConfig.LINGER_MS_CONFIG to 10,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to IntegerSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
    )

    @Bean

```

--------------------------------

### Add Kafka Topics with Error Handling (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

Introduces a version of the `addTopics` method that returns a `Map<String, Exception>` for improved error handling. This allows capturing and inspecting exceptions for each topic creation attempt, differentiating between success (null value) and failure.

```java
Map<String, Exception> results = embeddedKafkaRule.getEmbeddedKafka()
        .addTopics(new NewTopic("topic-with-error", 1, (short) 1));
```

--------------------------------

### Configuring Message Listener Containers in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Demonstrates how to configure message listener containers for receiving messages from Kafka topics using Spring Kafka. This includes setting up container factories and listener properties.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<Object, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Additional configurations can be added here, e.g., concurrency, error handling
        return factory;
    }
}
```

--------------------------------

### Configure KafkaJaasLoginModuleInitializer for Kerberos

Source: https://docs.spring.io/spring-kafka/reference/kafka/kerberos

This Java snippet demonstrates how to configure the KafkaJaasLoginModuleInitializer bean. It sets the control flag and provides a map of Kerberos-specific options such as keytab path, principal, and store key settings. This is essential for enabling JAAS and Kerberos authentication in Spring Kafka.

```java
@Bean
public KafkaJaasLoginModuleInitializer jaasConfig() throws IOException {
    KafkaJaasLoginModuleInitializer jaasConfig = new KafkaJaasLoginModuleInitializer();
    jaasConfig.setControlFlag("REQUIRED");
    Map<String, String> options = new HashMap<>();
    options.put("useKeyTab", "true");
    options.put("storeKey", "true");
    options.put("keyTab", "/etc/security/keytabs/kafka_client.keytab");
    options.put("principal", "kafka-client-1@EXAMPLE.COM");
    jaasConfig.setOptions(options);
    return jaasConfig;
}
```

--------------------------------

### Kafka Streams API Usage (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

Demonstrates the basic usage of the Apache Kafka Streams API within a Java application. It shows how to build a processing topology using StreamsBuilder and manage the lifecycle of the KafkaStreams instance.

```java
StreamsBuilder builder = ...;  // when using the Kafka Streams DSL

// Use the configuration to tell your application where the Kafka cluster is,
// which serializers/deserializers to use by default, to specify security settings,
// and so on.
StreamsConfig config = ...;

KafkaStreams streams = new KafkaStreams(builder, config);

// Start the Kafka Streams instance
streams.start();

// Stop the Kafka Streams instance
streams.close();
```

--------------------------------

### Configure Kafka Streams Support

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Provides guidance on integrating Apache Kafka Streams with Spring applications. This allows for building stream processing applications directly within the Spring ecosystem, leveraging Spring's dependency injection and configuration management.

```java
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaStreamsConfig {

    @Bean
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }

    @Bean
    public org.springframework.kafka.support.TopicBuilder<String, String> topicBuilder() {
        // Example: Defining topics for Kafka Streams
        return org.springframework.kafka.support.TopicBuilder;
    }

    // Define your Kafka Streams topology here, using the StreamsBuilder
    @Bean
    public void kafkaStreamsTopology(StreamsBuilder builder) {
        builder.stream("input-topic")
               .mapValues(value -> value.toUpperCase())
               .to("output-topic");
    }
}
```

--------------------------------

### Kafka Streams Configuration with Spring for Apache Kafka

Source: https://docs.spring.io/spring-kafka/reference/streams

This Java configuration sets up a Kafka Streams application within a Spring Boot environment. It defines the necessary properties for Kafka Streams, including bootstrap servers and serializers/deserializers. It also includes a state listener for monitoring stream state transitions and a KStream pipeline that processes messages from 'streamingTopic1', converts them to uppercase, groups them by key, applies windowed aggregation, filters based on value length, and outputs to 'streamingTopic2'.

```java
@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBeanConfigurer configurer() {
        return fb -> fb.setStateListener((newState, oldState) -> {
            System.out.println("State transition from " + oldState + " to " + newState);
        });
    }

    @Bean
    public KStream<Integer, String> kStream(StreamsBuilder kStreamBuilder) {
        KStream<Integer, String> stream = kStreamBuilder.stream("streamingTopic1");
        stream
                .mapValues((ValueMapper<String, String>) String::toUpperCase)
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(1_000)))
                .reduce((String value1, String value2) -> value1 + value2,
                		Named.as("windowStore"))
                .toStream()
                .map((windowedId, value) -> new KeyValue<>(windowedId.key(), value))
                .filter((i, s) -> s.length() > 40)
                .to("streamingTopic2");

        stream.print(Printed.toSysOut());

        return stream;
    }

}

```

--------------------------------

### Configure Embedded Kafka Broker with Properties and Placeholders (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

This snippet shows how to use the @EmbeddedKafka annotation with advanced configurations, including specifying topics using property placeholders, defining broker properties directly, and loading broker properties from an external file. It demonstrates property placeholder resolution from the Spring Environment and external configuration files. Dependencies include Spring Test and Spring Kafka.

```java
@TestPropertySource(locations = "classpath:/test.properties")
@EmbeddedKafka(topics = { "any-topic", "${kafka.topics.another-topic}" },
        brokerProperties = { "log.dir=${kafka.broker.logs-dir}",
                            "listeners=PLAINTEXT://localhost:${kafka.broker.port}",
                            "auto.create.topics.enable=${kafka.broker.topics-enable:true}" },
        brokerPropertiesLocation = "classpath:/broker.properties")

```

--------------------------------

### Configure Embedded Kafka Broker in Test Class - Java

Source: https://docs.spring.io/spring-kafka/reference/testing

This Java code snippet demonstrates how to configure an `EmbeddedKafkaBroker` within a test class to use the shared broker managed by `EmbeddedKafkaHolder`. It initializes the broker and adds topics to it during static initialization. The `broker.getBrokersAsString()` method can be used to obtain bootstrap servers if not using Spring Boot.

```java
static {
    EmbeddedKafkaHolder.getEmbeddedKafka().addTopics("topic1", "topic2");
}

private static final EmbeddedKafkaBroker broker = EmbeddedKafkaHolder.getEmbeddedKafka();
```

--------------------------------

### Hamcrest Matchers for Kafka ConsumerRecords

Source: https://docs.spring.io/spring-kafka/reference/testing

Provides Hamcrest matchers for verifying properties of Kafka ConsumerRecords. These matchers are useful in testing scenarios to assert the key, value, partition, or timestamp of a consumed record. They do not have external dependencies beyond standard Java and Kafka libraries.

```java
/**
 * @param key the key
 * @param <K> the type.
 * @return a Matcher that matches the key in a consumer record.
 */
public static <K> Matcher<ConsumerRecord<K, ?>> hasKey(K key) { ... }

/**
 * @param value the value.
 * @param <V> the type.
 * @return a Matcher that matches the value in a consumer record.
 */
public static <V> Matcher<ConsumerRecord<?, V>> hasValue(V value) { ... }

/**
 * @param partition the partition.
 * @return a Matcher that matches the partition in a consumer record.
 */
public static Matcher<ConsumerRecord<?, ?>> hasPartition(int partition) { ... }

/**
 * Matcher testing the timestamp of a {@link ConsumerRecord} assuming the topic has been set with
 * {@link org.apache.kafka.common.record.TimestampType#CREATE_TIME CreateTime}.
 *
 * @param ts timestamp of the consumer record.
 * @return a Matcher that matches the timestamp in a consumer record.
 */
public static Matcher<ConsumerRecord<?, ?>> hasTimestamp(long ts) {
  return hasTimestamp(TimestampType.CREATE_TIME, ts);
}

/**
 * Matcher testing the timestamp of a {@link ConsumerRecord}
 * @param type timestamp type of the record
 * @param ts timestamp of the consumer record.
 * @return a Matcher that matches the timestamp in a consumer record.
 */
public static Matcher<ConsumerRecord<?, ?>> hasTimestamp(TimestampType type, long ts) {
  return new ConsumerRecordTimestampMatcher(type, ts);
}
```

--------------------------------

### Configuring KafkaListenerContainerFactory with Reply Template and Headers

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This Java Bean configuration demonstrates setting up a ConcurrentKafkaListenerContainerFactory. It includes setting a KafkaTemplate for replies and configuring a ReplyHeadersConfigurer to selectively copy headers. This is essential for @SendTo functionality.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<Integer, String> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<Integer, String> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf());
    factory.setReplyTemplate(template());
    factory.setReplyHeadersConfigurer((k, v) -> k.equals("cat"));
    return factory;
}
```

--------------------------------

### Define KafkaTemplate Bean with ProducerFactory (Kotlin)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This snippet demonstrates how to programmatically define a KafkaTemplate bean in a Spring application using Kotlin. It requires a ProducerFactory to be pre-configured. This approach is necessary when Spring Boot's auto-configuration is not utilized.

```kotlin
fun kafkaTemplate(producerFactory: ProducerFactory<Int, String>) = KafkaTemplate(producerFactory)
```

--------------------------------

### DefaultKafkaHeaderMapper Constructors

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Illustrates the different ways to instantiate the DefaultKafkaHeaderMapper. These constructors allow for customization using a Jackson ObjectMapper and header mapping patterns.

```java
public DefaultKafkaHeaderMapper() { 
    ...
}

public DefaultKafkaHeaderMapper(ObjectMapper objectMapper) { 
    ...
}

public DefaultKafkaHeaderMapper(String... patterns) { 
    ...
}

public DefaultKafkaHeaderMapper(ObjectMapper objectMapper, String... patterns) { 
    ...
}
```

--------------------------------

### Sending Messages with KafkaTemplate

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

Demonstrates how to send messages to Kafka topics using `KafkaTemplate`. This is a core utility for producing messages in Spring Kafka applications. It requires a configured `KafkaTemplate` bean and typically takes the topic name and the message payload as input.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaMessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    public void sendMessageWithKey(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message);
    }
}
```

--------------------------------

### Send Messages using KafkaTemplate

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Illustrates how to use `KafkaTemplate` to send messages to Kafka topics. This is a fundamental operation for producing messages within a Spring Kafka application. It demonstrates sending simple strings and `ProducerRecord` objects.

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.kafka.clients.producer.ProducerRecord;

@Component
public class KafkaMessageSender {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
        future.addCallback(sendResult -> {
            System.out.println("Sent message=\"" + message + "\" to topic=\"" + topic + "\"");
        }, ex -> {
            System.err.println("Unable to send message=\"" + message + "\" to topic=\"" + topic + "\" due to: " + ex.getMessage());
        });
    }

    public void sendMessageWithRecord(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        future.addCallback(sendResult -> {
            System.out.println("Sent record with key=\"" + key + "\" and value=\"" + value + "\" to topic=\"" + topic + "\"");
        }, ex -> {
            System.err.println("Unable to send record with key=\"" + key + "\" to topic=\"" + topic + "\" due to: " + ex.getMessage());
        });
    }
}
```

--------------------------------

### Using KafkaTemplate to Receive Messages

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

This snippet illustrates a less common but possible pattern where `KafkaTemplate` can be used in conjunction with specific configurations to facilitate receiving messages, often in testing scenarios or for specific integration patterns. It leverages the send-and-receive capabilities of the template.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class KafkaSendAndReceiveService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaSendAndReceiveService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public ListenableFuture<SendResult<String, String>> sendAndReceive(String requestTopic, String replyTopic, String message) {
        // Send message to request topic and configure it to receive reply on replyTopic
        // This requires specific KafkaTemplate configuration, often involving correlation IDs
        // For simplicity, a basic send is shown here. Actual receive requires more setup.
        return kafkaTemplate.send(requestTopic, message);
    }

    // To actually receive, you would typically have a @KafkaListener configured for the replyTopic
    // that correlates responses back to the original request.
}

```

--------------------------------

### Custom KafkaTemplate for Sending Replies with Partition and Key

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This code defines a custom KafkaTemplate that overrides the send method to include logic for determining partition and key before sending the reply message. This is useful when simple send(topic, value) is not sufficient. It requires a producer factory.

```java
@Bean
public KafkaTemplate<String, String> myReplyingTemplate() {
    return new KafkaTemplate<String, String>(producerFactory()) {

        @Override
        public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
            return super.send(topic, partitionForData(data), keyForData(data), data);
        }

        ...

    };
}
```

--------------------------------

### Fetching Records from Kafka Consumer (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

Utility methods provided by KafkaTestUtils to fetch records from a Kafka consumer during testing. getSingleRecord polls for exactly one record, while getRecords polls for all available records.

```java
/**
 * Poll the consumer, expecting a single record for the specified topic.
 * @param consumer the consumer.
 * @param topic the topic.
 * @return the record.
 * @throws org.junit.ComparisonFailure if exactly one record is not received.
 */
public static <K, V> ConsumerRecord<K, V> getSingleRecord(Consumer<K, V> consumer, String topic) { ... }

/**
 * Poll the consumer for records.
 * @param consumer the consumer.
 * @return the records.
 */
public static <K, V> ConsumerRecords<K, V> getRecords(Consumer<K, V> consumer) { ... }
```

--------------------------------

### EmbeddedKafkaRule for JUnit 4 (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

A JUnit 4 Rule wrapper for EmbeddedKafkaZKBroker to set up an embedded Kafka and Zookeeper server. It allows configuration of the number of brokers, controlled shutdown, and topics to be created with specified partitions.

```java
/**
 * Create embedded Kafka brokers.
 * @param count the number of brokers.
 * @param controlledShutdown passed into TestUtils.createBrokerConfig.
 * @param topics the topics to create (2 partitions per).
 */
public EmbeddedKafkaRule(int count, boolean controlledShutdown, String... topics) { ... }

/**
 *
 * Create embedded Kafka brokers.
 * @param count the number of brokers.
 * @param controlledShutdown passed into TestUtils.createBrokerConfig.
 * @param partitions partitions per topic.
 * @param topics the topics to create.
 */
public EmbeddedKafkaRule(int count, boolean controlledShutdown, int partitions, String... topics) { ... }
```

--------------------------------

### Using KafkaTemplate for Sending Messages in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Illustrates how to use `KafkaTemplate` to send messages to Kafka topics within a Spring application. This is a common approach for producers.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }
}
```

--------------------------------

### Consuming from Embedded Kafka Topics (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

Demonstrates how to consume messages from all topics created by an EmbeddedKafkaBroker using a configured consumer. This utility is helpful for verifying message production in tests.

```java
Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
Consumer<Integer, String> consumer = cf.createConsumer();
embeddedKafka.consumeFromAllEmbeddedTopics(consumer);
```

--------------------------------

### Mock Consumer Factory Configuration for Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/testing

Configures a MockConsumerFactory for use with Spring Kafka listener containers. It sets up a MockConsumer with specific topic partitions and simulates polling tasks to add records. This allows for testing without an actual Kafka broker.

```java
@Bean
ConsumerFactory<String, String> consumerFactory() {
    MockConsumer<String, String> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    TopicPartition topicPartition0 = new TopicPartition("topic", 0);
    List<TopicPartition> topicPartitions = Collections.singletonList(topicPartition0);
    Map<TopicPartition, Long> beginningOffsets = topicPartitions.stream().collect(
            Collectors.toMap(Function.identity(), tp -> 0L));
    consumer.updateBeginningOffsets(beginningOffsets);
    consumer.schedulePollTask(() -> {
        consumer.addRecord(
                new ConsumerRecord<>("topic", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, null, "test1",
                        new RecordHeaders(), Optional.empty()));
        consumer.addRecord(
                new ConsumerRecord<>("topic", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, null, "test2",
                        new RecordHeaders(), Optional.empty()));
    });
    return new MockConsumerFactory(() -> consumer);
}
```

--------------------------------

### Configure Default RetryTopicConfiguration Bean

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

This snippet shows how to create a default RetryTopicConfiguration bean. It automatically sets up retry topics, DLQ, and consumers for Kafka listeners using a provided KafkaTemplate. This is useful for applying default retry behavior across all topics.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, Object> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .create(template);
}
```

--------------------------------

### Configure Listener Container Factory

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Shows how to configure a `ConcurrentKafkaListenerContainerFactory` to customize the behavior of Kafka listeners. This allows for fine-grained control over aspects like concurrency, error handling, and message conversion.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // Additional configurations can be added here, e.g., error handlers, concurrency settings
        return factory;
    }
}
```

--------------------------------

### Spring Kafka Test Utilities for Producer and Consumer Properties (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

Provides static methods in KafkaTestUtils to set up producer and consumer properties for testing with Spring Kafka and EmbeddedKafkaBroker. These methods help in configuring essential parameters like group IDs and auto-commit behavior.

```java
/**
 * Set up test properties for an {@code <Integer, String>} consumer.
 * @param group the group id.
 * @param autoCommit the auto commit.
 * @param embeddedKafka a {@link EmbeddedKafkaBroker} instance.
 * @return the properties.
 */
public static Map<String, Object> consumerProps(String group, String autoCommit,
                                       EmbeddedKafkaBroker embeddedKafka) { ... }

/**
 * Set up test properties for an {@code <Integer, String>} producer.
 * @param embeddedKafka a {@link EmbeddedKafkaBroker} instance.
 * @return the properties.
 */
public static Map<String, Object> producerProps(EmbeddedKafkaBroker embeddedKafka) { ... }
```

--------------------------------

### Mock Producer Factory Configuration for Spring Kafka (Transactional)

Source: https://docs.spring.io/spring-kafka/reference/testing

Configures a MockProducerFactory for creating transactional MockProducers. It initializes the producer for transactions and allows specifying a default transactional ID, enabling testing of transactional Kafka operations.

```java
@Bean
ProducerFactory<String, String> transFactory() {
    MockProducer<String, String> mockProducer = 
            new MockProducer<>(true, new StringSerializer(), new StringSerializer());
    mockProducer.initTransactions();
    return new MockProducerFactory<String, String>((tx, id) -> mockProducer, "defaultTxId");
}
```

--------------------------------

### Using KafkaTemplate to Receive in Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Explains how to use KafkaTemplate in a reactive or asynchronous manner to receive messages. This pattern is useful when you need to trigger a message send based on a prior operation or integrate with asynchronous processing flows. It leverages Spring's reactive programming support.

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component
public class KafkaTemplateReceiver {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaTemplateReceiver(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public ListenableFuture<SendResult<String, String>> receiveAndSend(String topic, String key, String payload) {
        return kafkaTemplate.send(topic, key, payload);
    }

    // Example of using it within another listener or service
    // @KafkaListener(topics = "triggerTopic")
    // public void triggerSend(String data) {
    //     receiveAndSend("targetTopic", "someKey", data + "-processed");
    // }
}
```

--------------------------------

### Sending Messages with KafkaTemplate

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Demonstrates how to send messages to Kafka topics using the KafkaTemplate. This is a core component for producing messages in Spring Kafka applications. It supports various overloads for sending different types of messages and handling results asynchronously.

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaMessageSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    // Example for sending with a key
    public void sendMessageWithKey(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message);
    }
}
```

--------------------------------

### Configuring Message Listeners with `@KafkaListener`

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

Demonstrates the basic usage of the `@KafkaListener` annotation to define message-driven endpoints. This annotation simplifies the creation of Kafka consumers by automatically managing listener containers and message consumption.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    @KafkaListener(topics = "myTopic", groupId = "myGroup")
    public void listen(String message) {
        System.out.println("Received Message: " + message);
    }
}
```

--------------------------------

### Add Spring Kafka Dependency (Gradle with Spring Boot)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This Gradle snippet shows how to declare the spring-kafka dependency without a version when using Spring Boot. Spring Boot will automatically provide a compatible version.

```gradle
implementation 'org.springframework.kafka:spring-kafka'
```

--------------------------------

### Sending Messages with KafkaTemplate in Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Demonstrates how to send messages to Kafka topics using the KafkaTemplate. This is a core component for publishing messages within a Spring Kafka application. It requires a configured KafkaTemplate bean.

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaMessageSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload);
    }

    public void sendMessage(String topic, String payload) {
        kafkaTemplate.send(topic, payload);
    }
}
```

--------------------------------

### Receive Messages with KafkaListener Annotation

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Demonstrates receiving messages from Kafka topics using the `@KafkaListener` annotation. This is the primary way to consume messages in Spring Kafka applications. It shows basic message reception and handling.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message) {
        System.out.println("Received message: " + message);
    }

    @KafkaListener(topics = "another-topic", groupId = "my-group")
    public void listenToAnotherTopic(String data) {
        System.out.println("Received data from another-topic: " + data);
    }
}
```

--------------------------------

### Configure Retry Topic Bean

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Shows how to programmatically configure retry topics using RetryTopicConfigurationBuilder, including specifying partitions and replication factors or disabling auto-creation.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .autoCreateTopicsWith(2, 3)
            .create(template);
}

@Bean
public RetryTopicConfiguration myOtherRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .doNotAutoCreateRetryTopics()
            .create(template);
}
```

--------------------------------

### Sending Messages with KafkaTemplate

Source: https://docs.spring.io/spring-kafka/reference/search

Demonstrates how to send messages to Kafka topics using the KafkaTemplate. This is a core component for producing messages within a Spring Kafka application. It handles serialization and topic routing.

```java
String topic = "my-topic";
String data = "my-data";

// Send a String message
kafkaTemplate.send(topic, data);

// Send a message with a key
Integer key = 1;
kafkaTemplate.send(topic, key, data);

// Send a message with a specific partition
Integer partition = 0;
kafkaTemplate.send(topic, partition, key, data);

// Send a message asynchronously and handle the result
kafkaTemplate.send(topic, data)
    .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

        @Override
        public void onSuccess(SendResult<String, String> result) {
            System.out.println("Sent message='" + data + "' with offset='" +
                    result.getRecordMetadata().offset() + "'\n");
        }

        @Override
        public void onFailure(Throwable ex) {
            System.out.println("Unable to send message='" + data + "'\n" +
                    ex.getMessage());
        }
    });
```

--------------------------------

### Configure KafkaStreamsInteractiveQueryService Bean

Source: https://docs.spring.io/spring-kafka/reference/streams

This snippet demonstrates how to configure `KafkaStreamsInteractiveQueryService` as a Spring bean. It requires a `StreamsBuilderFactoryBean` and returns an instance of the service, which acts as a facade for Kafka Streams' interactive query APIs.

```java
@Bean
public KafkaStreamsInteractiveQueryService kafkaStreamsInteractiveQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
    final KafkaStreamsInteractiveQueryService kafkaStreamsInteractiveQueryService =
            new KafkaStreamsInteractiveQueryService(streamsBuilderFactoryBean);
    return kafkaStreamsInteractiveQueryService;
}
```

--------------------------------

### Retry Topic Configuration using Bean and Fixed Backoff

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Defines a retryable topic configuration programmatically using a Spring bean. This approach allows for more detailed configuration of the retry mechanism, including specifying the backoff strategy (e.g., fixed delay) and the maximum number of attempts. It leverages `RetryTopicConfigurationBuilder` for fluent construction.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .fixedBackoff(3_000)
            .maxAttempts(4)
            .create(template);
}
```

--------------------------------

### Configuring Listener Container Factory

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Shows how to configure a KafkaListenerContainerFactory, which is used to create and manage listener containers. This allows for customization of various aspects of message consumption, such as concurrency, error handling, and deserialization.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3); // Example: set concurrency to 3
        // factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE); // Example: manual acknowledgement
        return factory;
    }

    // Assuming ConsumerFactory is configured elsewhere or defined as a bean
    // @Bean
    // public ConsumerFactory<String, String> consumerFactory() {
    //     Map<String, Object> props = new HashMap<>();
    //     props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    //     props.put(ConsumerConfig.GROUP_ID_CONFIG, "myGroup");
    //     props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    //     props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    //     return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(props);
    // }
}
```

--------------------------------

### Add Spring Kafka Dependency (Maven with Spring Boot)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

When using Spring Boot, this snippet shows how to declare the spring-kafka dependency without specifying the version. Spring Boot will manage the compatible version.

```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

--------------------------------

### Using `KafkaTemplate` to Send and Receive Messages

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

Shows how to use `KafkaTemplate` for sending messages to Kafka topics. While primarily for sending, it can also be used in scenarios where a listener might trigger a send operation or when testing message flows.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public MessageSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
        System.out.println("Sent message to topic '" + topic + "': " + message);
    }
}
```

--------------------------------

### Add Spring Kafka Dependency (Maven)

Source: https://docs.spring.io/spring-kafka/reference/3.1/quick-tour

This snippet shows how to add the spring-kafka dependency to a Maven project. It specifies the group ID, artifact ID, and version for the dependency.

```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
  <version>3.1.10</version>
</dependency>
```

--------------------------------

### AssertJ Conditions for Kafka ConsumerRecords

Source: https://docs.spring.io/spring-kafka/reference/testing

Offers AssertJ conditions for validating Kafka ConsumerRecord attributes. These conditions facilitate fluent assertions in tests, checking the key, value, partition, or timestamp of consumer records. They rely on AssertJ and standard Java libraries.

```java
/**
 * @param key the key
 * @param <K> the type.
 * @return a Condition that matches the key in a consumer record.
 */
public static <K> Condition<ConsumerRecord<K, ?>> key(K key) { ... }

/**
 * @param value the value.
 * @param <V> the type.
 * @return a Condition that matches the value in a consumer record.
 */
public static <V> Condition<ConsumerRecord<?, V>> value(V value) { ... }

/**
 * @param key the key.
 * @param value the value.
 * @param <K> the key type.
 * @param <V> the value type.
 * @return a Condition that matches the key in a consumer record.
 * @since 2.2.12
 */
public static <K, V> Condition<ConsumerRecord<K, V>> keyValue(K key, V value) { ... }

/**
 * @param partition the partition.
 * @return a Condition that matches the partition in a consumer record.
 */
public static Condition<ConsumerRecord<?, ?>> partition(int partition) { ... }

/**
 * @param value the timestamp.
 * @return a Condition that matches the timestamp value in a consumer record.
 */
public static Condition<ConsumerRecord<?, ?>> timestamp(long value) {
  return new ConsumerRecordTimestampCondition(TimestampType.CREATE_TIME, value);
}

/**
 * @param type the type of timestamp
 * @param value the timestamp.
 * @return a Condition that matches the timestamp value in a consumer record.
 */
public static Condition<ConsumerRecord<?, ?>> timestamp(TimestampType type, long value) {
  return new ConsumerRecordTimestampCondition(type, value);
}
```

--------------------------------

### Configuring KafkaListenerContainerFactory in Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Shows how to programmatically configure a Kafka listener container factory. This allows for customization of listener container properties such as thread pools, error handlers, and deserializers. It's essential for fine-tuning message consumption.

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "myGroup");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Add other configurations like error handlers, concurrency, etc.
        return factory;
    }
}
```

--------------------------------

### Configure Retryable Topic Annotation

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Demonstrates how to use the @RetryableTopic annotation to configure topic auto-creation with specific partition and replication factor settings, or to disable auto-creation.

```java
@RetryableTopic(numPartitions = 2, replicationFactor = 3)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}

@RetryableTopic(autoCreateTopics = false)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Configuring Listener Container Factory in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

Demonstrates how to configure a `ConcurrentKafkaListenerContainerFactory` to customize the behavior of Kafka listener containers. This allows for fine-grained control over aspects like concurrency, error handling, and deserialization.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaListenerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3); // Set the number of concurrent consumers
        // Add other configurations as needed, e.g., error handling, batch listeners
        return factory;
    }
}
```

--------------------------------

### Configure Retry and DLT for KafkaListener Method with @RetryableTopic

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Applies the @RetryableTopic annotation to a @KafkaListener annotated method to configure retry topics and DLTs. Spring for Apache Kafka will bootstrap necessary topics and consumers with default configurations. It requires a KafkaTemplate bean (or defaults to 'defaultRetryTopicKafkaTemplate').

```java
@RetryableTopic(kafkaTemplate = "myRetryableTopicKafkaTemplate")
@KafkaListener(topics = "my-annotated-topic", groupId = "myGroupId")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Handling Listener Exceptions with @KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Demonstrates how to configure exception handling for message listeners annotated with `@KafkaListener`. This includes specifying error channels or custom error handlers.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlingListener {

    @KafkaListener(topics = "errorTopic", groupId = "errorGroup", 
                   errorHandler = "myErrorHandler")
    public void processMessage(@Payload String message) {
        // Process message
        if (message.contains("error")) {
            throw new RuntimeException("Simulated error processing message: " + message);
        }
        System.out.println("Processed: " + message);
    }

    // Assuming 'myErrorHandler' is defined elsewhere as a Bean
}
```

--------------------------------

### KafkaListener Returning Message with Headers (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

Demonstrates a Kafka listener method that returns a Message<?> object. The listener is responsible for setting reply headers, including the reply topic and correlation ID, which is useful when interacting with components like ReplyingKafkaTemplate. Dependencies include Spring Kafka and its message building utilities.

```java
@KafkaListener(id = "messageReturned", topics = "someTopic")
public Message<?> listen(String in, @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTo,
        @Header(KafkaHeaders.CORRELATION_ID) byte[] correlation) {
    return MessageBuilder.withPayload(in.toUpperCase())
            .setHeader(KafkaHeaders.TOPIC, replyTo)
            .setHeader(KafkaHeaders.KEY, 42)
            .setHeader(KafkaHeaders.CORRELATION_ID, correlation)
            .setHeader("someOtherHeader", "someValue")
            .build();
}
```

--------------------------------

### Spring Kafka: Asserting Messages with AssertJ (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

This Java code snippet demonstrates how to use AssertJ for asserting messages received from a Kafka topic in a Spring Kafka test. It replaces Hamcrest matchers with AssertJ's fluent assertion style for checking message values, keys, and partitions. This requires the AssertJ library to be included in the project's dependencies.

```java
assertThat(records.poll(10, TimeUnit.SECONDS)).has(value("foo"));
template.sendDefault(0, 2, "bar");
ConsumerRecord<Integer, String> received = records.poll(10, TimeUnit.SECONDS);
// using individual assertions
assertThat(received).has(key(2));
assertThat(received).has(value("bar"));
assertThat(received).has(partition(0));
template.send(TEMPLATE_TOPIC, 0, 2, "baz");
received = records.poll(10, TimeUnit.SECONDS);
// using allOf()
assertThat(received).has(allOf(keyValue(2, "baz"), partition(0)));
```

--------------------------------

### KafkaListener with @SendTo and Static Reply Topic

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

Illustrates a KafkaListener that forwards its return value to a statically defined reply topic. An error handler is also specified, and a KafkaTemplate needs to be configured in the container factory.

```java
@KafkaListener(topics = "annotated23", errorHandler = "replyErrorHandler")
@SendTo("annotated23reply") // static reply topic definition
public String replyingListenerWithErrorHandler(String in) {
    ...
}
```

--------------------------------

### KafkaStreamsInteractiveQueryService API Signature (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

This shows the signature of the `getKafkaStreamsApplicationHostInfo` method from the `KafkaStreamsInteractiveQueryService`. It details the parameters required to query for host information: the state store name, the key to look up, and the serializer for that key. This method is fundamental for locating the correct Kafka Streams instance in a distributed system.

```java
public <K> HostInfo getKafkaStreamsApplicationHostInfo(String store, K key, Serializer<K> serializer)
```

--------------------------------

### Map Kafka Headers to MessageHeaders (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Demonstrates mapping Kafka Headers to MessageHeaders using DefaultKafkaHeaderMapper. It shows how to convert byte arrays and strings to their appropriate representations in MessageHeaders and verifies the mapping.

```java
headersMap.put("thisOnesBytes", "thing2");
    headersMap.put("alwaysRaw", "thing3".getBytes());
    MessageHeaders headers = new MessageHeaders(headersMap);
    Headers target = new RecordHeaders();
    mapper.fromHeaders(headers, target);
    assertThat(target).containsExactlyInAnyOrder(
            new RecordHeader("thisOnesAString", "thing1".getBytes()),
            new RecordHeader("thisOnesBytes", "thing2".getBytes()),
            new RecordHeader("alwaysRaw", "thing3".getBytes()));
    headersMap.clear();
    mapper.toHeaders(target, headersMap);
    assertThat(headersMap).contains(
            entry("thisOnesAString", "thing1"),
            entry("thisOnesBytes", "thing2".getBytes()),
            entry("alwaysRaw", "thing3".getBytes()));
```

--------------------------------

### KafkaStreamsInfrastructureCustomizer Interface

Source: https://docs.spring.io/spring-kafka/reference/streams

Defines the KafkaStreamsInfrastructureCustomizer interface for customizing StreamsBuilder and Topology before KafkaStreams are created. It provides methods to configure the builder and the topology, with default no-op implementations.

```java
public interface KafkaStreamsInfrastructureCustomizer {

    void configureBuilder(StreamsBuilder builder);

    void configureTopology(Topology topology);

}
```

--------------------------------

### Configure Message Converter for Serialization/Deserialization

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Explains how to configure message converters for handling serialization and deserialization of message payloads. This is crucial for interoperability between producers and consumers using different data formats like JSON or Avro.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConverterConfig {

    @Bean
    public RecordMessageConverter jsonMessageConverter() {
        // Configure Jackson ObjectMapper if needed for custom JSON settings
        return new JsonMessageConverter();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            RecordMessageConverter jsonMessageConverter) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }

    @Bean
    public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<String, String> producerFactory,
                                           RecordMessageConverter jsonMessageConverter) {
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setMessageConverter(jsonMessageConverter);
        return kafkaTemplate;
    }
}
```

--------------------------------

### Advanced Kafka Retry Topic Configuration

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Provides advanced configuration for Kafka retry topic components and global features by extending the `RetryTopicConfigurationSupport` class. This allows for overriding specific methods to customize behavior, such as setting retry container concurrency.

```java
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;

@Configuration
public class AdvancedKafkaConfig extends RetryTopicConfigurationSupport {

    @Override
    protected void configureContainerFactory(org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
        // Configure container factory properties here
        // For example, setting concurrency for retry containers:
        // factory.getContainerProperties().setConcurrency(3);
    }

    // ... other configurations
}
```

--------------------------------

### Configure Blocking Retries and Non-Blocking Fatal Exceptions in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

This Java code demonstrates how to configure blocking retries, including specific exceptions to retry on and back-off policies, and how to manage non-blocking fatal exceptions by adding them to a list. It uses the `RetryTopicConfigurationSupport` class for customization in Spring Kafka.

```java
@EnableKafka
@Configuration
public class MyRetryTopicConfiguration extends RetryTopicConfigurationSupport {

    @Override
    protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
        blockingRetries
                .retryOn(MyBlockingRetriesException.class, MyOtherBlockingRetriesException.class)
                .backOff(new FixedBackOff(3000, 3));
    }

    @Override
    protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
        nonBlockingFatalExceptions.add(MyNonBlockingException.class);
    }

    @Override
    protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
        // Use the new 2.9 mechanism to avoid re-fetching the same records after a pause
        customizersConfigurer.customizeErrorHandler(eh -> {
            eh.setSeekAfterError(false);
        });
    }

}
```

--------------------------------

### Autowire StreamsBuilderFactoryBean by Name using @Qualifier

Source: https://docs.spring.io/spring-kafka/reference/streams

Shows how to autowire a StreamsBuilderFactoryBean bean by its name using the "&" prefix and the @Qualifier annotation. This is useful when dealing with multiple instances or interface-based bean definitions.

```java
@Bean
public FactoryBean<StreamsBuilder> myKStreamBuilder(KafkaStreamsConfiguration streamsConfig) {
    return new StreamsBuilderFactoryBean(streamsConfig);
}
...
@Autowired
@Qualifier("&myKStreamBuilder")
private StreamsBuilderFactoryBean myKStreamBuilderFactoryBean;
```

--------------------------------

### Configure Embedded Kafka Broker Property

Source: https://docs.spring.io/spring-kafka/reference/testing

Allows specifying a custom system property to expose Kafka broker addresses. This is useful for integrating with Spring Boot's auto-configuration, ensuring the `spring.kafka.bootstrap-servers` property is correctly set before running tests with an embedded Kafka.

```properties
spring.embedded.kafka.brokers.property=spring.kafka.bootstrap-servers
```

--------------------------------

### Implement Custom Retry Topic Naming Provider (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

This Java code provides a `CustomRetryTopicNamesProviderFactory` that extends the default behavior by adding a "my-prefix-" prefix to retry and DLT topic names. It differentiates between the main endpoint and retry/DLT endpoints to apply the prefix selectively.

```java
public class CustomRetryTopicNamesProviderFactory implements RetryTopicNamesProviderFactory {

    @Override
    public RetryTopicNamesProvider createRetryTopicNamesProvider(
                DestinationTopic.Properties properties) {

        if (properties.isMainEndpoint()) {
            return new SuffixingRetryTopicNamesProvider(properties);
        }
        else {
            return new SuffixingRetryTopicNamesProvider(properties) {

                @Override
                public String getTopicName(String topic) {
                    return "my-prefix-" + super.getTopicName(topic);
                }

            };
        }
    }

}
```

--------------------------------

### Enable Kafka Retry Topic Annotation

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Enables the Kafka retry topic feature by adding the `@EnableKafkaRetryTopic` annotation to a configuration class. This annotation is meta-annotated with `@EnableKafka`, so explicit addition of `@EnableKafka` is not required. It facilitates proper bootstrapping and allows injection of retry components.

```java
@Configuration
@EnableKafkaRetryTopic
public class KafkaConfig {
    // ... other configurations
}
```

--------------------------------

### Autowire StreamsBuilderFactoryBean by Type

Source: https://docs.spring.io/spring-kafka/reference/streams

Demonstrates how to autowire a StreamsBuilderFactoryBean bean directly by its type. This is a straightforward method when only one instance of the bean exists.

```java
@Bean
public StreamsBuilderFactoryBean myKStreamBuilder(KafkaStreamsConfiguration streamsConfig) {
    return new StreamsBuilderFactoryBean(streamsConfig);
}
...
@Autowired
private StreamsBuilderFactoryBean myKStreamBuilderFactoryBean;
```

--------------------------------

### Include and Exclude Topics for Retry Configuration

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Specifies which topics should be handled by a RetryTopicConfiguration bean using includeTopic, includeTopics, excludeTopic, and excludeTopics methods.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .includeTopics(List.of("my-included-topic", "my-other-included-topic"))
            .create(template);
}

@Bean
public RetryTopicConfiguration myOtherRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .excludeTopic("my-excluded-topic")
            .create(template);
}
```

--------------------------------

### Create RetryTopicConfiguration for Annotated Class in Java

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Demonstrates how to programmatically create a RetryTopicConfiguration for a class annotated with @RetryableTopic. This is useful for providing custom retry configurations when the default behavior is not sufficient. It utilizes RetryTopicConfigurationProvider to find or create the appropriate configuration.

```java
@Bean
public RetryTopicConfiguration myRetryTopic() {
    RetryTopicConfigurationProvider provider = new RetryTopicConfigurationProvider(beanFactory);
    return provider.findRetryConfigurationFor(topics, null, AnnotatedClass.class, bean);
}

@RetryableTopic
public static class AnnotatedClass {
    // NoOps
}
```

--------------------------------

### KafkaListener with @SendTo using Configuration-Time SpEL Expression

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

This snippet shows a batch KafkaListener configured to send its results to a topic defined by a SpEL expression evaluated at configuration time, referencing a bean property. A KafkaTemplate must be available in the factory.

```java
@KafkaListener(topics = "${some.property:annotated22}")
@SendTo("#{myBean.replyTopic}") // config time SpEL
public Collection<String> replyingBatchListener(List<String> in) {
    ...
}
```

--------------------------------

### Manually Committing Offsets in Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Demonstrates the pattern for manually committing Kafka consumer offsets. This provides explicit control over message acknowledgment, which is crucial for ensuring exactly-once processing semantics or handling batch processing scenarios. It requires the listener container to be configured for manual acknowledgment.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualOffsetCommitListener {

    @KafkaListener(topics = "commitTopic", groupId = "commitGroup", containerFactory = "manualAckContainerFactory")
    public void listen(String message, Acknowledgment acknowledgment) {
        System.out.println("Received message: " + message);
        // Process the message
        // If processing is successful, commit the offset
        acknowledgment.acknowledge();
    }
}
```

--------------------------------

### Embedded Kafka Broker Implementations in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/testing

Provides an overview of the two embedded Kafka broker implementations available in the 'spring-kafka-test' jar. One uses Zookeeper (legacy), and the other uses Kraft (since 3.1) for Kafka's controller and broker modes.

```java
org.springframework.kafka.test.EmbeddedKafkaZKBroker
org.springframework.kafka.test.EmbeddedKafkaKraftBroker
```

--------------------------------

### Custom Producer Interceptor Implementation in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/interceptors

This Java class implements the Kafka ProducerInterceptor interface. It demonstrates how to receive and use a Spring Bean ('SomeBean') injected via the `configure` method using configuration properties. The `onSend` method shows how to access the injected bean and invoke its methods before the record is sent.

```java
public class MyProducerInterceptor implements ProducerInterceptor<String, String> {

    private SomeBean bean;

    @Override
    public void configure(Map<String, ?> configs) {
        this.bean = (SomeBean) configs.get("some.bean");
    }

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        this.bean.someMethod("producer interceptor");
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

}

```

--------------------------------

### Retry Topic Configuration with Custom Backoff Policy

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Demonstrates how to configure a retryable topic with a custom backoff policy by implementing Spring Retry's `SleepingBackOffPolicy` interface. This provides granular control over the retry delay intervals, allowing for complex backoff strategies beyond the built-in options. The custom policy is then provided to the `RetryTopicConfigurationBuilder`.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .customBackoff(new MyCustomBackOffPolicy())
            .maxAttempts(5)
            .create(template);
}
```

--------------------------------

### Receiving Messages with @KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/search

Illustrates how to consume messages from Kafka topics using the @KafkaListener annotation. This is the primary way to build message-driven consumers in Spring Kafka. It supports various configurations for message handling and offset management.

```java
@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listen(ConsumerRecord<String, String> record) {
    System.out.println("Received Message: " + record.value());
    // Process the message
}

@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listenWithHeaders(@Payload String message,
                            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
    System.out.println("Received: " + message + " from partition: " + partition);
    // Process the message
}

@KafkaListener(topics = "my-topic", groupId = "my-group", containerFactory = "manualAckContainerFactory")
public void listenManualAck(String data, Acknowledgment acknowledgment) {
    System.out.println("Received: " + data);
    // Process the message
    acknowledgment.acknowledge(); // Manually commit offset
}
```

--------------------------------

### Wire Spring Beans into Interceptors

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Demonstrates how to wire Spring beans into producer and consumer interceptors for Apache Kafka. This allows for centralized management and customization of interceptor logic, such as adding headers, logging, or metrics.

```java
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.adapter.AdapterRegistry;
import org.springframework.kafka.listener.adapter.KafkaMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.ListenerAdapter;
import org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter;

import java.util.List;
import java.util.Map;

// Example Spring Bean for a ProducerInterceptor
class MyProducerInterceptor implements ProducerInterceptor<String, String> {
    @Override
    public void onSend(Map<String, ?> configs, org.apache.kafka.clients.producer.ProducerRecord<String, String> record) {
        System.out.println("Producer interceptor: Sending record to topic " + record.topic());
        // Add custom header for example
        record.headers().add("x-custom-header", "my-value".getBytes());
    }

    @Override
    public org.apache.kafka.clients.producer.RecordMetadata onAcknowledgement(org.apache.kafka.clients.producer.RecordMetadata metadata, Exception exception) {
        // Handle acknowledgement
        return metadata;
    }

    @Override
    public void close() {
        // Clean up resources
    }
}

// Example Spring Bean for a ConsumerInterceptor
class MyConsumerInterceptor implements ConsumerInterceptor<String, String> {
    @Override
    public void onCommit(Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsets) {
        // Handle commit
    }

    @Override
    public void onConsume(org.apache.kafka.clients.consumer.ConsumerRecords<String, String> records) {
        System.out.println("Consumer interceptor: Consumed records.");
        // Process records, potentially modifying them or logging
    }

    @Override
    public void close() {
        // Clean up resources
    }
}

@Configuration
public class KafkaInterceptorConfig {

    @Bean
    public ProducerInterceptor<String, String> producerInterceptor() {
        return new MyProducerInterceptor();
    }

    @Bean
    public ConsumerInterceptor<String, String> consumerInterceptor() {
        return new MyConsumerInterceptor();
    }

    // Configure ProducerFactory to use the producer interceptor
    @Bean
    public ProducerFactory<?, ?> producerFactory(org.springframework.kafka.core.ProducerFactory<?, ?> defaultProducerFactory) {
        // Assuming defaultProducerFactory is configured elsewhere
        Map<String, Object> producerConfigs = new java.util.HashMap<>(defaultProducerFactory.getProducerConfigs());
        producerConfigs.put("interceptor.classes", List.of(MyProducerInterceptor.class.getName()));
        return new org.springframework.kafka.core.DefaultProducerFactory<>(producerConfigs, defaultProducerFactory.getKeySerializer(), defaultProducerFactory.getValueSerializer());
    }

    // Configure ConsumerFactory to use the consumer interceptor
    @Bean
    public ConsumerFactory<?, ?> consumerFactory(org.springframework.kafka.core.ConsumerFactory<?, ?> defaultConsumerFactory) {
        // Assuming defaultConsumerFactory is configured elsewhere
        Map<String, Object> consumerConfigs = new java.util.HashMap<>(defaultConsumerFactory.getConsumerConfigs());
        consumerConfigs.put("interceptor.classes", List.of(MyConsumerInterceptor.class.getName()));
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(consumerConfigs, defaultConsumerFactory.getKeyDeserializer(), defaultConsumerFactory.getValueDeserializer());
    }
}
```

--------------------------------

### Forwarding Listener Results using @SendTo

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Illustrates how to use the `@SendTo` annotation with `@KafkaListener` to forward processed message results to another Kafka topic.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class ForwardingListener {

    @KafkaListener(topics = "inputTopic")
    @SendTo("outputTopic")
    public String processAndForward(String message) {
        // Process the message
        String processedMessage = "Forwarded: " + message.toUpperCase();
        return processedMessage;
    }
}
```

--------------------------------

### Helper Spring Bean Class in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/interceptors

A simple Java class representing a Spring Bean ('SomeBean') that can be injected into Kafka interceptors. It contains a basic method 'someMethod' used to demonstrate the successful injection and usage of the bean within the interceptor logic.

```java
public class SomeBean {

    public void someMethod(String what) {
        System.out.println(what + " in my foo bean");
    }

}

```

--------------------------------

### Configure RetryTopicConfiguration with Listener Factory Instance (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/retry-topic-lcf

This code defines a Spring '@Bean' for 'RetryTopicConfiguration'. It demonstrates how to programmatically set a specific 'ConcurrentKafkaListenerContainerFactory' instance to be used for the retry topic configuration. This approach offers flexibility in managing the lifecycle and configuration of the retry mechanism. It requires Spring Kafka and Spring Boot dependencies.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template,
        ConcurrentKafkaListenerContainerFactory<Integer, MyPojo> factory) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .listenerFactory(factory)
            .create(template);
}
```

--------------------------------

### Configure Embedded Kafka Broker with @EmbeddedKafka Annotation (Java)

Source: https://docs.spring.io/spring-kafka/reference/testing

This snippet demonstrates how to use the @EmbeddedKafka annotation to configure an embedded Kafka broker for testing. It specifies the number of partitions and the topics to be created. The annotation is typically used as a class-level rule in JUnit tests. Dependencies include Spring Test, Spring Kafka, and JUnit.

```java
@RunWith(SpringRunner.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1,
         topics = {
                 KafkaStreamsTests.STREAMING_TOPIC1,
                 KafkaStreamsTests.STREAMING_TOPIC2 })
public class KafkaStreamsTests {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    public void someTest() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", this.embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<Integer, String> consumer = cf.createConsumer();
        this.embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaStreamsTests.STREAMING_TOPIC2);
        ConsumerRecords<Integer, String> replies = KafkaTestUtils.getRecords(consumer);
        assertThat(replies.count()).isGreaterThanOrEqualTo(1);
    }

    @Configuration
    @EnableKafkaStreams
    public static class TestKafkaStreamsConfiguration {

        @Value("${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
        private String brokerAddresses;

        @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
        public KafkaStreamsConfiguration kStreamsConfigs() {
            Map<String, Object> props = new HashMap<>();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreams");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.brokerAddresses);
            return new KafkaStreamsConfiguration(props);
        }

    }

}
```

--------------------------------

### Configure Retry and DLT Suffixes Programmatically

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

This code snippet shows how to programmatically configure custom suffixes for retry and dead-letter topics using the `RetryTopicConfigurationBuilder`. This approach is useful when you need more control over the retry topic configuration outside of annotations.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyOtherPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .retryTopicSuffix("-my-retry-suffix")
            .dltTopicSuffix("-my-dlt-suffix")
            .create(template);
}
```

--------------------------------

### JSON Serialization and Deserialization with JsonSerde

Source: https://docs.spring.io/spring-kafka/reference/streams

Demonstrates how to use Spring Kafka's JsonSerde for serializing and deserializing messages in JSON format within Kafka Streams. It supports configurable target types and ObjectMapper instances. The fluent API simplifies programmatic configuration for producer/consumer factories.

```java
stream.through(Serdes.Integer(), new JsonSerde<>(Cat.class), "cats");
```

```java
stream.through(
    new JsonSerde<>(MyKeyType.class)
        .forKeys()
        .noTypeInfo(),
    new JsonSerde<>(MyValueType.class)
        .noTypeInfo(),
    "myTypes");
```

--------------------------------

### Embedded Broker with JUnit4 Class Rule and @SpringBootTest

Source: https://docs.spring.io/spring-kafka/reference/testing

This snippet illustrates using a JUnit4 ClassRule to create an embedded Kafka broker within a Spring Boot application test. It configures the broker and sets the bootstrap server property for Spring Boot.

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class MyApplicationTests {

    @ClassRule
    public static EmbeddedKafkaRule broker = new EmbeddedKafkaRule(1, false, "someTopic")
            .brokerListProperty("spring.kafka.bootstrap-servers");

    @Autowired
    private KafkaTemplate<String, String> template;

    @Test
    public void test() {
        ...
    }

}
```

--------------------------------

### Configure Micrometer Kafka Producer/Consumer Listeners in Spring

Source: https://docs.spring.io/spring-kafka/reference/kafka/micrometer

This snippet demonstrates how to configure Micrometer's Kafka listeners for both consumers and producers within a Spring application context. It involves setting up `DefaultKafkaConsumerFactory` and `DefaultKafkaProducerFactory` and adding `MicrometerConsumerListener` and `MicrometerProducerListener` respectively. These listeners require a `MeterRegistry` and can accept custom tags. The consumer/producer 'id' is automatically added as a tag 'spring.id'.

```java
@Bean
public ConsumerFactory<String, String> myConsumerFactory() {
    Map<String, Object> configs = consumerConfigs();
    ...
    DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(configs);
    ...
    cf.addListener(new MicrometerConsumerListener<String, String>(meterRegistry(),
            Collections.singletonList(new ImmutableTag("customTag", "customTagValue"))));
    ...
    return cf;
}

@Bean
public ProducerFactory<String, String> myProducerFactory() {
    Map<String, Object> configs = producerConfigs();
    configs.put(ProducerConfig.CLIENT_ID_CONFIG, "myClientId");
    ...
    DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(configs);
    ...
    pf.addListener(new MicrometerProducerListener<String, String>(meterRegistry(),
            Collections.singletonList(new ImmutableTag("customTag", "customTagValue"))));
    ...
    return pf;
}
```

--------------------------------

### Customize Dead Letter Publishing Recoverer

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Illustrates how to customize the DeadLetterPublishingRecoverer to control header management for failed messages, specifically appending original headers and handling previous exception headers.

```java
@Override
protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
    customizersConfigurer.customizeDeadLetterPublishingRecoverer(dlpr -> {
        dlpr.setAppendOriginalHeaders(true);
        dlpr.setStripPreviousExceptionHeaders(false);
    });
}
```

--------------------------------

### Configuring Manual Offset Committing

Source: https://docs.spring.io/spring-kafka/reference/search

Shows how to configure a Kafka listener container to manually commit offsets. This provides fine-grained control over when messages are considered processed, crucial for exactly-once processing semantics.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<?, ?> manualAckContainerFactory(
        ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
        ConsumerFactory<Object, Object> kafkaConsumerFactory) {

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    return factory;
}

@KafkaListener(topics = "my-topic", groupId = "my-group", containerFactory = "manualAckContainerFactory")
public void listenManualAck(String data, Acknowledgment acknowledgment) {
    System.out.println("Received: " + data);
    // Process the message
    acknowledgment.acknowledge(); // Manually commit offset
}
```

--------------------------------

### Configure DefaultErrorHandler seekAfterError in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/how-the-pattern-works

This snippet shows how to configure the `DefaultErrorHandler` to set `seekAfterError` to `false` when using manual `AckMode` with `asyncAcks` set to true. This was necessary for compatibility with earlier versions of Spring Kafka when using the default DLT handler.

```java
@Override
protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
    customizersConfigurer.customizeErrorHandler(eh -> eh.setSeekAfterError(false));
}
```

--------------------------------

### Handle Exceptions in Kafka Listener

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Details strategies for handling exceptions that occur during message processing by a Kafka listener. This includes configuring error handlers to manage failed message deliveries, retries, and dead-letter queueing.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingBrokerErrorHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.ExponentialBackOffWithJitter;

@Component
public class ErrorHandlingListener {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "my-error-topic", groupId = "my-error-group", 
                   errorHandler = "myErrorHandler")
    public void listen(String message) {
        System.out.println("Processing message: " + message);
        if (message.contains("error")) {
            throw new RuntimeException("Simulated processing error");
        }
        // Process message successfully
    }

    // Define a custom error handler bean
    @Bean
    public CommonErrorHandler myErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingBrokerErrorHandler errorHandler = new DeadLetterPublishingBrokerErrorHandler(template);
        
        // Configure retry mechanism
        errorHandler.setBackOff(new FixedBackOff(1000L, 5)); // 1 second delay, 5 retries
        
        // Or use exponential backoff
        // errorHandler.setBackOff(new ExponentialBackOffWithJitter(1000L, 2.0, 0.1));
        
        return errorHandler;
    }
}
```

--------------------------------

### KafkaHeaderMapper Interface Definition

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Defines the strategy for mapping headers between Kafka's Headers object and Spring's MessageHeaders. It provides methods for converting headers in both directions.

```java
public interface KafkaHeaderMapper {

    void fromHeaders(MessageHeaders headers, Headers target);

    void toHeaders(Headers source, Map<String, Object> target);

}
```

--------------------------------

### Single Topic for Fixed Delay Retries with RetryTopic

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

Configure RetryTopic to use a single topic for fixed delay retries, preventing the appending of index or delay values. This is achieved using `SameIntervalTopicReuseStrategy.SINGLE_TOPIC`. The `FixedDelayStrategy` is deprecated and replaced by `SameIntervalTopicReuseStrategy`.

```java
@RetryableTopic(backoff = @Backoff(2_000), sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .fixedBackOff(3_000)
            .maxAttempts(5)
            .useSingleTopicForSameIntervals()
            .create(template);
}
```

--------------------------------

### Configure KafkaListener to Handle Null Payloads (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/tombstones

This configuration demonstrates how to set up a `@KafkaListener` to handle null payloads, which signifies key deletion in compacted logs. It uses `@Payload(required = false)` to allow null values and captures the received key.

```java
@KafkaListener(id = "deletableListener", topics = "myTopic")
public void listen(@Payload(required = false) String value, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    // value == null represents key deletion
}
```

--------------------------------

### Register Custom Retry Topic Naming Strategy (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

This code snippet demonstrates how to register a custom `RetryTopicNamesProviderFactory` within a Spring component factory to override the default retry topic naming strategy. It shows the structure for providing a custom implementation of `retryTopicNamesProviderFactory()`.

```java
@Override
protected RetryTopicComponentFactory createComponentFactory() {
    return new RetryTopicComponentFactory() {
        @Override
        public RetryTopicNamesProviderFactory retryTopicNamesProviderFactory() {
            return new CustomRetryTopicNamesProviderFactory();
        }
    };
}
```

--------------------------------

### Receiving Messages with @KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Illustrates how to consume messages from Kafka topics using the @KafkaListener annotation. This is the primary mechanism for creating message-driven consumers in Spring Kafka. It supports asynchronous processing and flexible configuration through listener container factories.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageListener {

    @KafkaListener(topics = "myTopic", groupId = "myGroup")
    public void listen(String message) {
        System.out.println("Received message: " + message);
    }

    // Example for receiving with message metadata
    @KafkaListener(topics = "anotherTopic", groupId = "anotherGroup")
    public void listenWithMetadata(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) {
        System.out.println("Received message: " + record.value() +
                           " from topic " + record.topic() +
                           " partition " + record.partition() +
                           " offset " + record.offset());
    }
}
```

--------------------------------

### Control Kafka Listener Container Pause/Resume in Spring Boot

Source: https://docs.spring.io/spring-kafka/reference/kafka/pause-resume

This Spring Boot application demonstrates how to pause and resume Kafka listener containers programmatically using the `KafkaListenerEndpointRegistry`. It sends messages, pauses the listener, sends another message (which won't be processed while paused), resumes the listener, and then processes the second message. It also shows how to capture `ConsumerPausedEvent` and `ConsumerResumedEvent`.

```java
@SpringBootApplication
public class Application implements ApplicationListener<KafkaEvent> {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args).close();
    }

    @Override
    public void onApplicationEvent(KafkaEvent event) {
        System.out.println(event);
    }

    @Bean
    public ApplicationRunner runner(KafkaListenerEndpointRegistry registry,
            KafkaTemplate<String, String> template) {
        return args -> {
            template.send("pause.resume.topic", "thing1");
            Thread.sleep(10_000);
            System.out.println("pausing");
            registry.getListenerContainer("pause.resume").pause();
            Thread.sleep(10_000);
            template.send("pause.resume.topic", "thing2");
            Thread.sleep(10_000);
            System.out.println("resuming");
            registry.getListenerContainer("pause.resume").resume();
            Thread.sleep(10_000);
        };
    }

    @KafkaListener(id = "pause.resume", topics = "pause.resume.topic")
    public void listen(String in) {
        System.out.println(in);
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("pause.resume.topic")
            .partitions(2)
            .replicas(1)
            .build();
    }

}
```

--------------------------------

### Configure KafkaTemplate with Producer Interceptor in Spring

Source: https://docs.spring.io/spring-kafka/reference/kafka/producer-interceptor-managed-in-spring

This Spring Java configuration demonstrates how to register a custom `MyProducerInterceptor` as a Spring bean and then set it on a `KafkaTemplate`. This approach allows Spring to manage the interceptor's lifecycle and dependencies. It requires a `ProducerFactory` bean and the custom interceptor bean to be available.

```java
@Bean
public MyProducerInterceptor myProducerInterceptor(SomeBean someBean) {
  return new MyProducerInterceptor(someBean);
}

@Bean
public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf, MyProducerInterceptor myProducerInterceptor) {
   KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(pf);
   kafkaTemplate.setProducerInterceptor(myProducerInterceptor);
}
```

--------------------------------

### Add KafkaStreamsMicrometerListener for Monitoring

Source: https://docs.spring.io/spring-kafka/reference/streams

Illustrates how to add a KafkaStreamsMicrometerListener to a StreamsBuilderFactoryBean to automatically register Micrometer meters for KafkaStreams metrics. This requires a MeterRegistry and optionally custom tags.

```java
streamsBuilderFactoryBean.addListener(new KafkaStreamsMicrometerListener(meterRegistry,
        Collections.singletonList(new ImmutableTag("customTag", "customTagValue"))));
```

--------------------------------

### Implement Kafka Producer Interceptor in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/producer-interceptor-managed-in-spring

This Java code defines a custom producer interceptor that implements the `ProducerInterceptor` interface. It demonstrates how to inject dependencies via the constructor and override methods like `onSend`, `onAcknowledgement`, and `close` for custom logic during message production. This interceptor can be managed by Spring.

```java
public class MyProducerInterceptor implements ProducerInterceptor<String, String> {

    private final SomeBean bean;

    public MyProducerInterceptor(SomeBean bean) {
        this.bean = bean;
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        this.bean.someMethod("producer interceptor");
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

}
```

--------------------------------

### Configure Retry and DLT Suffixes with @KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

This code snippet demonstrates how to configure custom suffixes for retry and dead-letter topics using the `@RetryableTopic` annotation on a `@KafkaListener`. This allows for specific naming conventions for topics involved in message retry mechanisms.

```java
@RetryableTopic(retryTopicSuffix = "-my-retry-suffix", dltTopicSuffix = "-my-dlt-suffix")
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Configure RetryTopicConfiguration with Listener Factory Bean Name (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/retry-topic-lcf

This snippet shows how to create a Spring '@Bean' for 'RetryTopicConfiguration' by referencing a listener container factory by its bean name. This allows for decoupling the configuration from the actual factory bean definition. It's useful when the factory is managed elsewhere or needs to be specified indirectly. Requires Spring Kafka and Spring Boot.

```java
@Bean
public RetryTopicConfiguration myOtherRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .listenerFactory("my-retry-topic-factory")
            .create(template);
}
```

--------------------------------

### Accessing Delivery Attempt Headers in KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/accessing-delivery-attempts

This code snippet demonstrates how to include delivery attempt headers directly in the `@KafkaListener` method signature. It shows how to declare parameters for both blocking and non-blocking delivery attempts. Blocking attempts are only available if configured, and non-blocking attempts may be null for the initial delivery.

```java
@Header(KafkaHeaders.DELIVERY_ATTEMPT) int blockingAttempts,
@Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer nonBlockingAttempts
```

--------------------------------

### Configure DLT Handler via Builder (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

Configures a DLT handler method using the RetryTopicConfigurationBuilder. This approach allows specifying the DLT processor bean name and method name programmatically. It requires a KafkaTemplate and a custom DLT processor class.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .dltHandlerMethod("myCustomDltProcessor", "processDltMessage")
            .create(template);
}

@Component
public class MyCustomDltProcessor {

    private final MyDependency myDependency;

    public MyCustomDltProcessor(MyDependency myDependency) {
        this.myDependency = myDependency;
    }

    public void processDltMessage(MyPojo message) {
        // ... message processing, persistence, etc
    }
}
```

--------------------------------

### Forwarding Listener Results with @SendTo (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

Demonstrates how to use the @SendTo annotation to forward the results of a Kafka listener to a specified destination topic. This is useful for routing processed messages to different topics for further processing or analysis. It requires the Spring Kafka dependency.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class MyKafkaListener {

    @KafkaListener(topics = "input-topic")
    @SendTo("output-topic")
    public String listenAndForward(String message) {
        // Process the message
        System.out.println("Received message: " + message);
        // Return the processed message to be forwarded
        return "Processed: " + message;
    }
}
```

--------------------------------

### Test Case for Specific String Header Conversion

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Demonstrates how to configure and use the DefaultKafkaHeaderMapper to handle specific string-valued headers as raw byte arrays instead of JSON. This is useful for interoperability with systems that do not support JSON headers.

```java
@Test
public void testSpecificStringConvert() {
    DefaultKafkaHeaderMapper mapper = new DefaultKafkaHeaderMapper();
    Map<String, Boolean> rawMappedHeaders = new HashMap<>();
    rawMappedHeaders.put("thisOnesAString", true);
    rawMappedHeaders.put("thisOnesBytes", false);
    mapper.setRawMappedHeaders(rawMappedHeaders);
    Map<String, Object> headersMap = new HashMap<>();
    headersMap.put("thisOnesAString", "thing1");
```

--------------------------------

### Configure KafkaStreamsInteractiveQueryService with Custom RetryTemplate

Source: https://docs.spring.io/spring-kafka/reference/streams

This code configures `KafkaStreamsInteractiveQueryService` with a custom `RetryTemplate`. It sets a `FixedBackOffPolicy` and a `SimpleRetryPolicy` with a maximum of ten attempts, overriding the default retry settings for state store retrieval.

```java
@Bean
public KafkaStreamsInteractiveQueryService kafkaStreamsInteractiveQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
    final KafkaStreamsInteractiveQueryService kafkaStreamsInteractiveQueryService =
            new KafkaStreamsInteractiveQueryService(streamsBuilderFactoryBean);
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(new FixedBackOffPolicy());
    RetryPolicy retryPolicy = new SimpleRetryPolicy(10);
    retryTemplate.setRetryPolicy(retryPolicy);
    kafkaStreamsInteractiveQueryService.setRetryTemplate(retryTemplate);
    return kafkaStreamsInteractiveQueryService;
}
```

--------------------------------

### Configure KafkaTemplate with DelegatingByTypeSerializer

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

This configuration sets up a KafkaTemplate with a ProducerFactory that uses DelegatingByTypeSerializer. This is crucial when using ErrorHandlingDeserializer for deserialization exceptions, ensuring the KafkaTemplate can handle both normal objects and raw byte[] values resulting from deserialization errors.

```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfiguration(), new StringSerializer(),
        new DelegatingByTypeSerializer(Map.of(byte[].class, new ByteArraySerializer(),
               MyNormalObject.class, new JsonSerializer<Object>())));
}

@Bean
public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

--------------------------------

### Configure DLT Failure Strategy: Fail on Error

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/dlt-strategies

This code shows how to configure the DLT processing to fail immediately when an error occurs, instead of retrying. This is achieved by setting `dltProcessingFailureStrategy` to `DltStrategy.FAIL_ON_ERROR` on the `@RetryableTopic` annotation.

```java
@RetryableTopic(dltProcessingFailureStrategy = DltStrategy.FAIL_ON_ERROR)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Manage Fatal Exceptions for DLT Delivery

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Allows adding custom exceptions to a global list that will cause records to be sent directly to the DLT without retries. This is achieved by overriding the manageNonBlockingFatalExceptions method.

```java
@Override
protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> nonBlockingFatalExceptions) {
    nonBlockingFatalExceptions.add(MyNonBlockingException.class);
}
```

--------------------------------

### Configure Multiple Kafka Listeners on Same Topic(s) - Java

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/multi-retry

This Java code demonstrates how to configure two separate Kafka listeners (`listener1` and `listener2`) to consume from the same topic (`TWO_LISTENERS_TOPIC`). It utilizes `@RetryableTopic` with custom suffixes for retry and DLT topics to ensure isolation between listeners. The `topicSuffixingStrategy` is set to `SUFFIX_WITH_INDEX_VALUE` for explicit control. This configuration is available from Spring for Apache Kafka version 3.0 onwards.

```java
@RetryableTopic(
        retryTopicSuffix = "-listener1", dltTopicSuffix = "-listener1-dlt",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
@KafkaListener(id = "listener1", groupId = "group1", topics = TWO_LISTENERS_TOPIC, ...)
void listen1(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    ...
}

@RetryableTopic(
        retryTopicSuffix = "-listener2", dltTopicSuffix = "-listener2-dlt",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
@KafkaListener(id = "listener2", groupId = "group2", topics = TWO_LISTENERS_TOPIC, ...)
void listen2(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    ...
}
```

--------------------------------

### Kafka Header Interface Definition

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Defines the basic structure of a Kafka header, consisting of a key and a byte array value. This is the fundamental interface for representing headers in Kafka messages.

```java
public interface Header {

    String key();

    byte[] value();

}
```

--------------------------------

### Manually Committing Offsets in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Details how to manually commit Kafka consumer offsets after processing messages, providing fine-grained control over message acknowledgment. Requires specific listener container configuration.

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualCommitListener implements AcknowledgingMessageListener<String, String> {

    @Override
    @KafkaListener(topics = "manualCommitTopic", groupId = "manualGroup")
    public void onMessage(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        System.out.println("Processing message: " + data.value());
        // Process the message
        
        // Manually commit the offset after successful processing
        acknowledgment.acknowledge();
    }
}
```

--------------------------------

### Annotated Kafka Listener with Retry and Backoff

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Configures a Kafka listener with retry attempts and a specific backoff strategy using annotations. The `@RetryableTopic` annotation handles retries, while `@Backoff` defines the delay, multiplier, and maximum delay for retries. This is useful for handling transient message processing failures.

```java
@RetryableTopic(attempts = 5,
    backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Configure RecoveringDeserializationExceptionHandler in Spring Kafka (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

This configuration sets up the RecoveringDeserializationExceptionHandler for Kafka Streams, directing deserialization errors to a DeadLetterPublishingRecoverer. It requires Kafka Streams properties and a KafkaTemplate bean.

```java
@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
public KafkaStreamsConfiguration kStreamsConfigs() {
    Map<String, Object> props = new HashMap<>();
    ...
    props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
            RecoveringDeserializationExceptionHandler.class);
    props.put(RecoveringDeserializationExceptionHandler.KSTREAM_DESERIALIZATION_RECOVERER, recoverer());
    ...
    return new KafkaStreamsConfiguration(props);
}

@Bean
public DeadLetterPublishingRecoverer recoverer() {
    return new DeadLetterPublishingRecoverer(kafkaTemplate(),
            (record, ex) -> new TopicPartition("recovererDLQ", -1));
}

```

--------------------------------

### Define KStream bean using StreamsBuilder

Source: https://docs.spring.io/spring-kafka/reference/streams

Defines a KStream bean by obtaining a StreamsBuilder instance from the application context. This allows for defining KStream instances before the application context is refreshed, especially when autoStartup is enabled.

```java
@Bean
public KStream<?, ?> kStream(StreamsBuilder kStreamBuilder) {
    KStream<Integer, String> stream = kStreamBuilder.stream(STREAMING_TOPIC1);
    // Fluent KStream API
    return stream;
}
```

--------------------------------

### Manually Committing Offsets

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Explains how to manually commit Kafka consumer offsets, which is crucial for ensuring exactly-once processing semantics or when fine-grained control over message acknowledgment is needed. This involves configuring the listener container to use manual acknowledgment mode.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualOffsetListener {

    @KafkaListener(topics = "manualAckTopic", groupId = "manualAckGroup")
    public void listen(String message, Acknowledgment ack) {
        System.out.println("Received message: " + message);
        // Process the message...
        
        // Manually acknowledge the message after successful processing
        ack.acknowledge(); 
    }
}
```

--------------------------------

### Non-Blocking Retries Configuration

Source: https://docs.spring.io/spring-kafka/reference/spring-projects

Details the configuration options for implementing non-blocking retries for message processing failures. This pattern is essential for building resilient Kafka consumers that can handle transient errors without blocking the entire consumer thread.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ExponentialBackOffWithMaxRetriesConfigurer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // Configure non-blocking retries with exponential backoff
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        
        // Optional: Configure dead-letter-topic properties if needed
        // See documentation for DeadLetterPublishingRecoverer
        
        // Optional: Set a fixed back-off for immediate retries
        // errorHandler.setBackOff(new FixedBackOff(1000L, 5));

        return errorHandler;
    }

    // You would then configure your KafkaListenerContainerFactory to use this error handler
    // For example:
    // @Bean
    // public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
    //         ConsumerFactory<?, ?> consumerFactory, CommonErrorHandler kafkaErrorHandler) {
    //     ConcurrentKafkaListenerContainerFactory<?, ?> factory = new ConcurrentKafkaListenerContainerFactory<>();
    //     factory.setConsumerFactory(consumerFactory);
    //     factory.setCommonErrorHandler(kafkaErrorHandler);
    //     factory.getContainerProperties().setAckMode(AckMode.RECORD);
    //     return factory;
    // }
}
```

--------------------------------

### Custom Consumer Interceptor Implementation in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/interceptors

This Java class implements the Kafka ConsumerInterceptor interface. Similar to the producer interceptor, it shows how to receive and utilize a Spring Bean ('SomeBean') injected through the `configure` method via configuration properties. The `onConsume` method illustrates calling a method on the injected bean when records are consumed.

```java
public class MyConsumerInterceptor implements ConsumerInterceptor<String, String> {

    private SomeBean bean;

    @Override
    public void configure(Map<String, ?> configs) {
        this.bean = (SomeBean) configs.get("some.bean");
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        this.bean.someMethod("consumer interceptor");
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
    }

    @Override
    public void close() {
    }

}

```

--------------------------------

### Receiving Messages with @KafkaListener in Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Illustrates how to define message listeners using the @KafkaListener annotation. This is the primary mechanism for consuming messages from Kafka topics in Spring Kafka. It requires a Kafka listener container factory to be configured.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageListener {

    @KafkaListener(topics = "myTopic", groupId = "myGroup")
    public void listen(String message) {
        System.out.println("Received Message: " + message);
    }

    @KafkaListener(topics = "anotherTopic", groupId = "anotherGroup", containerFactory = "kafkaListenerContainerFactory")
    public void listenWithSpecificFactory(String message) {
        System.out.println("Received Message on another topic: " + message);
    }
}
```

--------------------------------

### Conditional Branching with KafkaStreamBrancher

Source: https://docs.spring.io/spring-kafka/reference/streams

Illustrates the use of KafkaStreamBrancher for creating conditional branches in Kafka Streams, offering a more concise alternative to manual KStream branching. It allows defining multiple branches based on predicates and a default branch.

```java
KStream<String, String>[] branches = builder.stream("source").branch(
        (key, value) -> value.contains("A"),
        (key, value) -> value.contains("B"),
        (key, value) -> true
);
branches[0].to("A");
branches[1].to("B");
branches[2].to("C");
```

```java
new KafkaStreamBrancher<String, String>()
        .branch((key, value) -> value.contains("A"), ks -> ks.to("A"))
        .branch((key, value) -> value.contains("B"), ks -> ks.to("B"))
        //default branch should not necessarily be defined in the end of the chain!
        .defaultBranch(ks -> ks.to("C"))
        .onTopOf(builder.stream("source"));
        //onTopOf method returns the provided stream so we can continue with method chaining
```

--------------------------------

### Override DeadLetterPublishingRecovererFactory with Custom DLPR Creator

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Demonstrates how to override the configureDeadLetterPublishingContainerFactory method to provide a custom DeadLetterPublisherCreator. This allows for the creation of a custom DeadLetterPublishingRecoverer (CustomDLPR) instance, enabling modifications to producer record creation for retry or dead-letter topics. It's recommended to use provided resolvers when constructing the custom instance.

```java
@Override
protected Consumer<DeadLetterPublishingRecovererFactory>
        configureDeadLetterPublishingContainerFactory() {

    return (factory) -> factory.setDeadLetterPublisherCreator(
            (templateResolver, destinationResolver) ->
                    new CustomDLPR(templateResolver, destinationResolver));
}
```

--------------------------------

### Using KafkaMessageHeaderAccessor for Delivery Attempts

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/accessing-delivery-attempts

This snippet shows how to inject `KafkaMessageHeaderAccessor` into a `@KafkaListener` method to simplify access to delivery attempt headers. The accessor provides methods like `getBlockingRetryDeliveryAttempt()` and `getNonBlockingRetryDeliveryAttempt()`. It will throw an exception for blocking retries if not enabled, and returns 1 for initial non-blocking deliveries.

```java
@RetryableTopic(backoff = @Backoff(...))
@KafkaListener(id = "dh1", topics = "dh1")
void listen(Thing thing, KafkaMessageHeaderAccessor accessor) {
    // ... use accessor.getBlockingRetryDeliveryAttempt() and accessor.getNonBlockingRetryDeliveryAttempt()
}
```

--------------------------------

### Configure KafkaListener for Batch Processing with Record Adapter

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/transactions

This Java configuration sets up a ConcurrentKafkaListenerContainerFactory to process messages in batches but adapt it to handle records individually using a DefaultBatchToRecordAdapter. This allows for finer-grained error handling within a batch, where individual records can be recovered or processed based on exceptions. The adapter is configured with a ConsumerRecordRecoverer to capture failed records.

```java
public static class TestListener {

    final List<String> values = new ArrayList<>();

    @KafkaListener(id = "batchRecordAdapter", topics = "test")
    public void listen(String data) {
        values.add(data);
        if ("bar".equals(data)) {
            throw new RuntimeException("reject partial");
        }
    }

}

@Configuration
@EnableKafka
public static class Config {

    ConsumerRecord<?, ?> failed;

    @Bean
    public TestListener test() {
        return new TestListener();
    }

    @Bean
    public ConsumerFactory<?, ?> consumerFactory() {
        return mock(ConsumerFactory.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setBatchToRecordAdapter(new DefaultBatchToRecordAdapter<>((record, ex) ->  {
            this.failed = record;
        }));
        return factory;
    }

}
```

--------------------------------

### Configure Blocking Retries in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/retry-topic-combine-blocking

This code snippet demonstrates how to configure blocking retries for specific exceptions in Spring Kafka by overriding the `configureBlockingRetries` method. It specifies exceptions to retry and a back-off strategy using `FixedBackOff`. This is useful for exceptions that are likely to cause repeated failures.

```java
@Override
protected void configureBlockingRetries(BlockingRetriesConfigurer blockingRetries) {
    blockingRetries
            .retryOn(MyBlockingRetryException.class, MyOtherBlockingRetryException.class)
            .backOff(new FixedBackOff(3_000, 5));
}
```

--------------------------------

### Non-Blocking Retries Configuration

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

This code demonstrates how to configure non-blocking retries for message processing failures. It involves setting up a retry mechanism that doesn't block the consumer thread, allowing it to continue processing other messages. This is essential for improving throughput and responsiveness in high-load scenarios. Configuration typically involves defining an `ExponentialBackOffPolicy`.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        // Configure retry/no-retry policies as needed
        return retryTemplate;
    }

    // Example of how to apply this to a listener container factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            RetryTemplate retryTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRetryTemplate(retryTemplate);
        factory.setRecoveryCallback(new ErrorHandling.); // Implement recovery logic
        factory.getContainerProperties().setAckMode(AckMode.RECORD);
        return factory;
    }
}

```

--------------------------------

### Retry Deliveries in Kafka Listener

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Details how to configure retry mechanisms for message deliveries within a Kafka listener. This is essential for handling transient failures and ensuring message processing robustness. It covers both blocking and non-blocking retry strategies.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class RetryConfig {

    @Bean
    public ErrorHandler kafkaErrorHandler() {
        // Retry up to 5 times with a fixed delay of 1000 milliseconds
        return new SeekToCurrentErrorHandler(
            new FixedBackOff(1000L, 5)
        );
    }

    // Apply this error handler to a specific listener container factory
    // Or configure it globally via KafkaListenerConfigurer if needed.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            ErrorHandler kafkaErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
```

--------------------------------

### Use @RetryableTopic as a Meta-Annotation

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Shows how to use @RetryableTopic as a meta-annotation to create custom annotations. This allows for reuse of retry configurations and can abstract away details like concurrency settings.

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RetryableTopic
static @interface MetaAnnotatedRetryableTopic {

    @AliasFor(attribute = "concurrency", annotation = RetryableTopic.class)
    String parallelism() default "3";

}
```

--------------------------------

### Configure Retry for Class-Level KafkaListener with @RetryableTopic

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Demonstrates using @RetryableTopic as a class-level annotation for @KafkaListener. This requires specifying the listener container factory. It configures retry mechanisms for all @KafkaHandler methods within the class.

```java
@RetryableTopic(listenerContainerFactory = "my-retry-topic-factory")
@KafkaListener(topics = "my-annotated-topic")
public class ClassLevelRetryListener {

    @KafkaHandler
    public void processMessage(MyPojo message) {
        // ... message processing
    }

}
```

--------------------------------

### Custom MockProducer Implementation in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/testing

Provides a custom MockProducer implementation that overrides the close method to do nothing. This is beneficial for testing scenarios that involve multiple sends on the same producer instance without it being closed between operations.

```java
@Bean
MockProducer<String, String> mockProducer() {
    return new MockProducer<>(false, new StringSerializer(), new StringSerializer()) {
        @Override
        public void close() {

        }
    };
}

@Bean
ProducerFactory<String, String> mockProducerFactory(MockProducer<String, String> mockProducer) {
    return new MockProducerFactory<>(() -> mockProducer);
}
```

--------------------------------

### Generic Type Signature for RetrieveQueryableStore API

Source: https://docs.spring.io/spring-kafka/reference/streams

This snippet displays the generic type signature of the `retrieveQueryableStore` method within `KafkaStreamsInteractiveQueryService`. The `<T>` indicates that the method can return any type `T`, allowing flexibility for different state store types.

```java
public <T> T retrieveQueryableStore(String storeName, QueryableStoreType<T> storeType)
```

--------------------------------

### Manually Committing Kafka Consumer Offsets

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

Illustrates how to manually commit Kafka consumer offsets within a message listener. This provides finer control over message acknowledgment, ensuring messages are only considered processed after explicit commitment.

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualCommitListener implements AcknowledgingConsumerAwareMessageListener<String, String> {

    @Override
    public void onMessage(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        System.out.println("Received: " + data.value());
        // Process the message...
        acknowledgment.acknowledge(); // Manually commit the offset
    }
}
```

--------------------------------

### Manually Commit Offsets in Kafka Listener

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/receiving-messages/sequencing

Demonstrates how to manually commit Kafka consumer offsets within a message listener. This approach provides explicit control over when messages are considered processed and committed, which is crucial for achieving reliable message processing.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualOffsetCommitListener {

    @KafkaListener(topics = "my-commit-topic", groupId = "my-commit-group")
    public void listen(String message, Acknowledgment ack) {
        System.out.println("Received message for manual commit: " + message);
        // Process the message...
        
        // Manually acknowledge the message
        ack.acknowledge();
        System.out.println("Message committed.");
    }
}
```

--------------------------------

### Receiving Messages with @KafkaListener

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

Illustrates the use of the `@KafkaListener` annotation to create message-driven consumers. This is the primary way to consume messages in Spring Kafka. It allows defining topics, group IDs, and other listener-specific properties. The annotated method receives the message payload.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageListener {

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message) {
        System.out.println("Received Message: " + message);
    }

    @KafkaListener(topics = "another-topic", groupId = "my-group")
    public void listenWithHeaders(org.springframework.messaging.Message<String> message) {
        System.out.println("Received Message: " + message.getPayload());
        System.out.println("Headers: " + message.getHeaders());
    }
}
```

--------------------------------

### Specify Listener Container Factory in @RetryableTopic (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/retry-topic-lcf

This snippet demonstrates how to use the 'listenerContainerFactory' attribute within the '@RetryableTopic' annotation to specify a custom bean name for the listener container factory. This factory will be used to create the listener containers for the retry and dead-letter topics. It requires the Spring for Apache Kafka dependency.

```java
@RetryableTopic(listenerContainerFactory = "my-retry-topic-factory")
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Asynchronous `@KafkaListener` Return Types

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/receiving-messages/sequencing

Explains how to handle asynchronous return types with `@KafkaListener`, enabling a listener to return a value that will be sent to a different topic. This is useful for chaining Kafka operations or for handling results asynchronously.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class AsyncListener {

    @KafkaListener(topics = "inputTopic")
    @SendTo("outputTopic")
    public String listenAndSend(String message) {
        System.out.println("Received: " + message + ", sending to output topic.");
        return "Processed: " + message;
    }
}
```

--------------------------------

### Non-Blocking Retries Configuration

Source: https://docs.spring.io/spring-kafka/reference/search

Defines how to configure non-blocking retries for message processing failures. This pattern prevents blocking consumer threads during retry attempts, improving throughput and responsiveness.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
        ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
        ConsumerFactory<Object, Object> kafkaConsumerFactory) {

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);

    // Configure non-blocking retries
    factory.setRetryTemplate(retryTemplate());
    factory.setRecoveryCallback(new DeadLetterPublishingRecoverer(kafkaTemplate()));

    return factory;
}

@Bean
public RetryTemplate retryTemplate() {
    return new RetryTemplateBuilder()
            .fixedBackoff(1000, 5000, 2)
            .build();
}

@Bean
public KafkaTemplate<Object, Object> kafkaTemplate() {
    // KafkaTemplate configuration...
    return new KafkaTemplate<>(producerFactory());
}

@Bean
public ProducerFactory<Object, Object> producerFactory() {
    // ProducerFactory configuration...
    return new DefaultKafkaProducerFactory<>(producerConfigs());
}

@Bean
public Map<String, Object> producerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return props;
}
```

--------------------------------

### Test with @EmbeddedKafka and JUnit5 Parameter Resolver

Source: https://docs.spring.io/spring-kafka/reference/testing

This snippet shows how to use the @EmbeddedKafka annotation with JUnit5 when not using the Spring test context. The EmbeddedKafkaCondition creates a broker and includes a parameter resolver to access the broker in the test method.

```java
@EmbeddedKafka
public class EmbeddedKafkaConditionTests {

    @Test
    public void test(EmbeddedKafkaBroker broker) {
        String brokerList = broker.getBrokersAsString();
        ...
    }

}
```

--------------------------------

### Restart Container on Fenced Reason

Source: https://docs.spring.io/spring-kafka/reference/kafka/events

Demonstrates how to restart a listener container when a ConsumerStoppedEvent is published with the reason 'FENCED'. This is useful for handling transactional producer fencing scenarios.

```java
if (event.getReason().equals(Reason.FENCED)) {
    event.getSource(MessageListenerContainer.class).start();
}
```

--------------------------------

### Configure Dead-Letter Publishing Container Factory in Spring Kafka

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

This Java code snippet shows how to configure a `DeadLetterPublishingRecovererFactory` within Spring Kafka. It specifically demonstrates how to set a custom partition resolver for the dead-letter publishing process. This is useful when you need fine-grained control over where records are sent after retries are exhausted.

```java
@EnableKafka
@Configuration
public class Config extends RetryTopicConfigurationSupport {

    @Override
    protected Consumer<DeadLetterPublishingRecovererFactory> configureDeadLetterPublishingContainerFactory() {
        return dlprf -> dlprf.setPartitionResolver((cr, nextTopic) -> null);
    }

    ...

}
```

--------------------------------

### Default `@KafkaHandler` with Discrete Header Access (Limitation)

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/class-level-kafkalistener

Shows an attempt to access a discrete header within a default `@KafkaHandler` method, highlighting a limitation where this approach might not work as expected due to Spring's argument resolution.

```java
@KafkaHandler(isDefault = true)
public void listenDefault(Object object, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    ...
}
```

--------------------------------

### Create Inbound-Only Kafka Header Mapper with Patterns (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/headers

Shows how to create a DefaultKafkaHeaderMapper that only processes inbound headers, using patterns to include or exclude specific header names. This is useful for filtering headers during message consumption.

```java
public static DefaultKafkaHeaderMapper forInboundOnlyWithMatchers(String... patterns) {
}

public static DefaultKafkaHeaderMapper forInboundOnlyWithMatchers(ObjectMapper objectMapper, String... patterns) {
}

public static SimpleKafkaHeaderMapper forInboundOnlyWithMatchers(String... patterns) {
}

DefaultKafkaHeaderMapper inboundMapper = DefaultKafkaHeaderMapper.forInboundOnlyWithMatchers("!abc*", "*");
```

--------------------------------

### Configure DLT Handler with @DltHandler Annotation

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/dlt-strategies

This snippet demonstrates how to specify a DLT processing method using the `@DltHandler` annotation on a method within a class annotated with `@RetryableTopic`. The same DLT handler method is applied to all `@RetryableTopic` annotated methods in the class.

```java
@RetryableTopic
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}

@DltHandler
public void processDltMessage(MyPojo message) {
    // ... message processing, persistence, etc
}
```

--------------------------------

### Specify Exceptions for Retries using Annotation and Bean

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Defines which exceptions should trigger retries and whether to traverse nested causes. This can be configured using the @RetryableTopic annotation or the RetryTopicConfigurationBuilder.

```java
@RetryableTopic(include = {MyRetryException.class, MyOtherRetryException.class}, traversingCauses = true)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    throw new RuntimeException(new MyRetryException()); // will retry
}
```

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyOtherPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .notRetryOn(MyDontRetryException.class)
            .create(template);
}
```

--------------------------------

### KafkaListener with @SendTo and Error Handler (Java)

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/annotation-send-to

Illustrates a Kafka listener method declared as void but configured with @SendTo to forward results or errors to a specific topic. It also shows how to define a custom KafkaListenerErrorHandler to manage exceptions during message processing and potentially send failure information to a designated topic.

```java
@KafkaListener(id = "voidListenerWithReplyingErrorHandler", topics = "someTopic",
        errorHandler = "voidSendToErrorHandler")
@SendTo("failures")
public void voidListenerWithReplyingErrorHandler(String in) {
    throw new RuntimeException("fail");
}

@Bean
public KafkaListenerErrorHandler voidSendToErrorHandler() {
    return (m, e) -> {
        return ... // some information about the failure and input data
    };
}
```

--------------------------------

### Java: Multiple Kafka Listeners on Same Topic with Custom Retry Topics

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/multi-retry

Demonstrates configuring two distinct Kafka listeners (`listener1` and `listener2`) to consume messages from the same topic (`TWO_LISTENERS_TOPIC`). Each listener is configured with unique retry and dead-letter topic suffixes using `@RetryableTopic` and `TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE` to ensure their retry mechanisms do not conflict. This approach is available from Spring Kafka version 3.0.

```java
@RetryableTopic(... 
        retryTopicSuffix = "-listener1", dltTopicSuffix = "-listener1-dlt", 
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
@KafkaListener(id = "listener1", groupId = "group1", topics = TWO_LISTENERS_TOPIC, ...)
void listen1(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    ...
}

@RetryableTopic(... 
        retryTopicSuffix = "-listener2", dltTopicSuffix = "-listener2-dlt", 
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
@KafkaListener(id = "listener2", groupId = "group2", topics = TWO_LISTENERS_TOPIC, ...)
void listen2(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic) {
    ...
}
```

--------------------------------

### Consume Kafka Listener Events with @KafkaListener and @EventListener

Source: https://docs.spring.io/spring-kafka/reference/kafka/events

Demonstrates how to use both @KafkaListener to receive messages and @EventListener to handle Kafka-related events like ListenerContainerIdleEvent. The @EventListener includes a condition to filter events based on the listener ID, addressing scenarios where multiple concurrent listeners exist.

```java
public class Listener {

    @KafkaListener(id = "qux", topics = "annotated")
    public void listen4(@Payload String foo, Acknowledgment ack) {
        ...
    }

    @EventListener(condition = "event.listenerId.startsWith('qux-')")
    public void eventHandler(ListenerContainerIdleEvent event) {
        ...
    }

}
```

--------------------------------

### Append Topic Index or Delay with RetryTopic

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/topic-naming

Configure RetryTopic to append either the index value or delay value to the topic suffix. This is useful for distinguishing retry attempts or delays. The default behavior appends delay values, except for fixed delay configurations with multiple topics, where index values are used.

```java
@RetryableTopic(topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .suffixTopicsWithIndexValues()
            .create(template);
    }
```

--------------------------------

### Configure DLT Handler with @DltHandler Annotation (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

Specifies a method to handle messages sent to the Dead Letter Topic (DLT) using the @DltHandler annotation. This method is invoked for all @RetryableTopic annotated methods within the same class. It requires no external dependencies beyond the message type.

```java
@RetryableTopic
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}

@DltHandler
public void processMessage(MyPojo message) {
    // ... message processing, persistence, etc
}
```

--------------------------------

### Filtering Messages in Spring Kafka Listener

Source: https://docs.spring.io/spring-kafka/reference/kafka/exactly-once

Shows how to configure message filtering for `@KafkaListener` methods, allowing only messages that meet specific criteria to be processed.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FilteringListener {

    @KafkaListener(topics = "filterTopic", filter = "messageFilter")
    public void listen(String message) {
        System.out.println("Received filtered message: " + message);
    }

    // Assuming 'messageFilter' is defined as a Spring Bean (e.g., a Predicate)
    // Example: @Bean public java.util.function.Predicate<String> messageFilter() {
    //     return msg -> msg.startsWith("important");
    // }
}
```

--------------------------------

### Class-Level `@KafkaListener` with Method-Level `@KafkaHandler`

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/class-level-kafkalistener

Demonstrates how to apply `@KafkaListener` to a class and use `@KafkaHandler` on methods to route messages based on their converted payload type. A default handler can be specified using `isDefault = true`.

```java
@KafkaListener(id = "multi", topics = "myTopic")
static class MultiListenerBean {

    @KafkaHandler
    public void listen(String foo) {
        ...
    }

    @KafkaHandler
    public void listen(Integer bar) {
        ...
    }

    @KafkaHandler(isDefault = true)
    public void listenDefault(Object object) {
        ...
    }

}
```

--------------------------------

### Accessing Delivery Attempts with KafkaMessageHeaderAccessor (Java)

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/accessing-delivery-attempts

This Java snippet shows how to use `KafkaMessageHeaderAccessor` to conveniently access blocking and non-blocking delivery attempt information within a `@KafkaListener` method. This accessor simplifies the process of retrieving retry attempt details and provides helper methods for accessing these values. Ensure blocking retries are enabled for `getBlockingRetryDeliveryAttempt()`.

```java
@RetryableTopic(backoff = @Backoff(...))
@KafkaListener(id = "dh1", topics = "dh1")
void listen(Thing thing, KafkaMessageHeaderAccessor accessor) {
    // Use accessor.getBlockingRetryDeliveryAttempt()
    // and accessor.getNonBlockingRetryDeliveryAttempt()
}
```

--------------------------------

### Messaging Function Interface for Spring Kafka (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

The MessagingFunction interface is used by the MessagingProcessor to interact with Spring Messaging components. It defines a single method 'exchange' that takes a Spring Message and returns a Spring Message.

```java
@FunctionalInterface
public interface MessagingFunction {

    Message<?> exchange(Message<?> message);

}

```

--------------------------------

### Handling Thread State Cleanup with @EventListener

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/kafka/thread-safety

This code snippet demonstrates how to use the `@EventListener` annotation to consume `ConsumerStoppedEvent` and clean up thread-local state or remove thread-scoped beans. This is crucial for managing resources when listener threads exit.

```java
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.kafka.event.ConsumerStoppedEvent;

@Component
public class ThreadStateCleanupListener {

    @EventListener
    public void handleConsumerStopped(ConsumerStoppedEvent event) {
        // Clean up ThreadLocal instances or remove thread-scoped beans
        // Example: ThreadLocal<?> myThreadLocal = ...;
        // myThreadLocal.remove();
        System.out.println("Consumer stopped, cleaning up thread state for group: " + event.getGroupId());
    }
}
```

--------------------------------

### Default `@KafkaHandler` with `ConsumerRecordMetadata` for Record Information

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/class-level-kafkalistener

Provides the recommended way to access record metadata, such as the topic, within a default `@KafkaHandler` method by using the `ConsumerRecordMetadata` type.

```java
@KafkaHandler(isDefault = true)
void listen(Object in, @Header(KafkaHeaders.RECORD_METADATA) ConsumerRecordMetadata meta) {
    String topic = meta.topic();
    ...
}
```

--------------------------------

### Add Headers to Kafka Streams Records with HeaderEnricherProcessor (Java)

Source: https://docs.spring.io/spring-kafka/reference/streams

The HeaderEnricherProcessor allows adding headers to Kafka Streams records. Header values are evaluated as SpEL expressions, which can access the record, key, value, and context. Expressions must return a byte array or a String.

```java
.process(() -> new HeaderEnricherProcessor(expressions))

```

```java
.process(() -> new HeaderEnricherProcessor<..., ...>(expressionMap))

```

```java
Map<String, Expression> headers = new HashMap<>();
headers.put("header1", new LiteralExpression("value1"));
SpelExpressionParser parser = new SpelExpressionParser();
headers.put("header2", parser.parseExpression("record.timestamp() + ' @' + record.offset()"));
ProcessorSupplier supplier = () -> new HeaderEnricher<String, String>(headers);
KStream<String, String> stream = builder.stream(INPUT);
stream
        .process(() -> supplier)
        .to(OUTPUT);

```

--------------------------------

### Manually Committing Offsets

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/access-topic-info-runtime

This snippet shows how to manually commit Kafka consumer offsets, offering greater control over message acknowledgment. It involves configuring the listener container factory to disable automatic commits and then using the provided `Acknowledgment` object within the listener method. This is crucial for ensuring exactly-once processing or when dealing with complex processing logic.

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ManualOffsetCommitListener {

    @KafkaListener(topics = "manual-commit-topic", groupId = "manual-group", containerFactory = "manualAckListenerContainerFactory")
    public void listen(String message, Acknowledgment ack) {
        System.out.println("Processing message: " + message);
        // Perform processing logic
        // If processing is successful, commit the offset
        ack.acknowledge();
    }
}

// Corresponding configuration in a @Configuration class:
/*
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> manualAckListenerContainerFactory(ConcurrentKafkaMessageListenerContainerFactory<?, ?> factory) {
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}*/

```

--------------------------------

### Specify DLT Handler Method with @DltHandler

Source: https://docs.spring.io/spring-kafka/reference/3.2-SNAPSHOT/retrytopic/retry-config

Defines a specific method within the same class to process messages sent to the Dead Letter Topic (DLT). If no @DltHandler is provided, a default consumer logs the DLT messages.

```java
@DltHandler
public void processMessage(MyPojo message) {
    // ... message processing, persistence, etc
}
```

--------------------------------

### Disable DLT Programmatically with RetryTopicConfiguration

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

This snippet demonstrates how to disable the DLT configuration for retryable topics programmatically. It uses the `RetryTopicConfigurationBuilder` to create a retry configuration and explicitly calls `doNotConfigureDlt()` to achieve the desired behavior. This method is useful when defining retry configurations as Spring beans.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .doNotConfigureDlt()
            .create(template);
}
```

--------------------------------

### Configure Idle Event Interval for ConcurrentKafkaListenerContainerFactory

Source: https://docs.spring.io/spring-kafka/reference/kafka/events

Configures the idleEventInterval for a ConcurrentKafkaListenerContainerFactory, enabling the publication of ListenerContainerIdleEvent when the listener is idle. This is useful for detecting periods of no message delivery. The interval is specified in milliseconds.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
    ...
    factory.getContainerProperties().setIdleEventInterval(60000L);
    ...
    return factory;
}
```

--------------------------------

### Set Global Timeout for Retries using Annotation and Bean

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/features

Configures a global timeout for the retry process. If the timeout is reached, messages are sent to the DLT or processing ends. This can be set using the @RetryableTopic annotation or via RetryTopicConfigurationBuilder.

```java
@RetryableTopic(backoff = @Backoff(2_000), timeout = 5_000)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<String, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .fixedBackoff(2_000)
            .timeoutAfter(5_000)
            .create(template);
}
```

--------------------------------

### Synchronize Kafka Transactions with Database Transactions

Source: https://docs.spring.io/spring-kafka/reference/3.1/kafka/transactions

This snippet demonstrates how to use Spring's @Transactional annotation to synchronize KafkaTemplate operations with a DataSourceTransactionManager. The KafkaTemplate automatically participates in the active transaction, ensuring atomicity between sending records and updating the database. The commits are ordered as DB commit followed by Kafka commit.

```java
@Transactional
public void process(List<Thing> things) {
    things.forEach(thing -> this.kafkaTemplate.send("topic", thing));
    updateDb(things);
}
```

--------------------------------

### Async @KafkaListener with CompletableFuture in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/async-returns

Demonstrates a Kafka listener method that returns a CompletableFuture. This allows the reply to be sent asynchronously after the future completes. The AckMode is automatically set to MANUAL when async return types are detected.

```java
@KafkaListener(id = "myListener", topics = "myTopic")
public CompletableFuture<String> listen(String data) {
    ...
    CompletableFuture<String> future = new CompletableFuture<>();
    future.complete("done");
    return future;
}
```

--------------------------------

### Async @KafkaListener with Mono in Java

Source: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/async-returns

Shows a Kafka listener method returning a Mono<Void>, suitable for asynchronous operations that do not produce a return value. The message is acknowledged when the Mono completes. Exception handling is managed by the container's error handler.

```java
@KafkaListener(id = "myListener", topics = "myTopic")
public Mono<Void> listen(String data) {
    ...
    return Mono.empty();
}
```

--------------------------------

### Set DLT Failure Strategy to Fail on Error (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

Configures the Dead Letter Topic (DLT) processing to fail immediately if an error occurs during DLT message handling. This is achieved by setting the dltProcessingFailureStrategy to DltStrategy.FAIL_ON_ERROR in the @RetryableTopic annotation. The default behavior is to always retry on error.

```java
@RetryableTopic(dltProcessingFailureStrategy =
            DltStrategy.FAIL_ON_ERROR)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Configure Idle Event Interval for KafkaMessageListenerContainer

Source: https://docs.spring.io/spring-kafka/reference/kafka/events

Sets the idleEventInterval on a KafkaMessageListenerContainer to publish a ListenerContainerIdleEvent after a specified period of inactivity. This allows for actions to be taken when no messages are received for a duration. The interval is set in milliseconds.

```java
@Bean
public KafkaMessageListenerContainer(ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties containerProps = new ContainerProperties("topic1", "topic2");
    ...
    containerProps.setIdleEventInterval(60000L);
    ...
    KafkaMessageListenerContainer<String, String> container = new KafKaMessageListenerContainer<>(consumerFactory, containerProps);
    return container;
}
```

--------------------------------

### Configure No DLT with Annotation - Java

Source: https://docs.spring.io/spring-kafka/reference/retrytopic/dlt-strategies

This snippet demonstrates how to disable DLT for a Kafka listener using the @RetryableTopic annotation with DltStrategy.NO_DLT. It's useful when you want retried messages to be dropped after exhaustion instead of being sent to a separate error topic.

```java
@RetryableTopic(dltProcessingFailureStrategy = DltStrategy.NO_DLT)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Disable DLT with @RetryableTopic Annotation

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

This snippet shows how to prevent a DLT from being configured for a Kafka listener by using the `dltProcessingFailureStrategy` attribute within the `@RetryableTopic` annotation. This approach is suitable for Kafka listeners defined with annotations.

```java
@RetryableTopic(dltProcessingFailureStrategy =
            DltStrategy.NO_DLT)
@KafkaListener(topics = "my-annotated-topic")
public void processMessage(MyPojo message) {
    // ... message processing
}
```

--------------------------------

### Disable DLT Retry on Failure via Builder (Java)

Source: https://docs.spring.io/spring-kafka/reference/3.1/retrytopic/dlt-strategies

Configures the retry topic behavior to not retry processing a message if it fails in the DLT handler. This is done using the doNotRetryOnDltFailure() method on the RetryTopicConfigurationBuilder. This effectively means failures in the DLT handler will not result in the message being re-processed by the DLT handler.

```java
@Bean
public RetryTopicConfiguration myRetryTopic(KafkaTemplate<Integer, MyPojo> template) {
    return RetryTopicConfigurationBuilder
            .newInstance()
            .dltHandlerMethod("myCustomDltProcessor", "processDltMessage")
            .doNotRetryOnDltFailure()
            .create(template);
}
```

=== COMPLETE CONTENT === This response contains all available snippets from this library. No additional content exists. Do not make further requests.