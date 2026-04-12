package ru.yandex.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@Configuration
@ConfigurationProperties("collector.kafka")
public class KafkaConfig {
    private ProducerConfig producer;

    public enum TopicType {
        USER_ACTIONS;

        public static TopicType from(String type) {
            for (TopicType value : values()) {
                if (value.name().equals(type.toUpperCase().replace('-', '_'))) {
                    return value;
                }
            }
            throw new IllegalArgumentException(String.format("Неизвестный тип топика: %s", type));
        }
    }

    @Getter
    @Setter
    public static class ProducerConfig {
        private Properties properties;
        private Map<String, String> topics;

        public Map<TopicType, String> getTypedTopics() {
            Map<TopicType, String> result = new EnumMap<>(TopicType.class);
            if (topics != null) {
                for (Map.Entry<String, String> entry : topics.entrySet()) {
                    result.put(TopicType.from(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
    }
}
