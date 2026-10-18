package com.safewalk.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${safewalk.kafka.topics.sos-events:sos.events}")
    private String sosEventsTopicName;

    @Bean
    public NewTopic sosEventsTopic() {
        return TopicBuilder.name(sosEventsTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
