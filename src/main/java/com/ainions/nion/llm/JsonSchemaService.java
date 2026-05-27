package com.ainions.nion.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaService {

    private final ObjectMapper objectMapper;

    public JsonSchemaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<ValidationMessage> validate(String schemaJson, String payloadJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            JsonSchema schema = factory.getSchema(schemaNode);
            JsonNode payload = objectMapper.readTree(payloadJson);
            return schema.validate(payload);
        } catch (Exception ex) {
            return Set.of();
        }
    }
}
