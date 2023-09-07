package com.jumbo.trus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jumbo.trus.serializer.CustomDateDeserializer;
import com.jumbo.trus.serializer.ValidationFieldSerializer;
import com.jumbo.trus.service.helper.ValidationField;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(ValidationField.class, new ValidationFieldSerializer());
        module.addDeserializer(Date.class, new CustomDateDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
