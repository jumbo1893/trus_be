package com.jumbo.trus.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jumbo.trus.serializer.CustomDateDeserializer;
import com.jumbo.trus.serializer.ValidationFieldSerializer;
import com.jumbo.trus.service.helper.ValidationField;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Date;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder
                .timeZone(TimeZone.getTimeZone("Europe/Prague"))
                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(new JavaTimeModule())
                .build()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);;
        SimpleModule module = new SimpleModule();
        module.addSerializer(ValidationField.class, new ValidationFieldSerializer());
        module.addDeserializer(Date.class, new CustomDateDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
