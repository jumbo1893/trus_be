package com.jumbo.trus.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jumbo.trus.service.helper.ValidationField;

import java.io.IOException;

public class ValidationFieldSerializer extends JsonSerializer<ValidationField> {

    @Override
    public void serialize(ValidationField field, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("field", field.getField());
        jsonGenerator.writeStringField("message", field.getMessage());
        jsonGenerator.writeEndObject();
    }
}
