package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.config.AiOpenAiProperties;
import com.jumbo.trus.service.exceptions.AiUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OpenAiMatchReportTest {
    @Test
    void sendsDedicatedInstructionsWithoutToolsAndRejectsTruncatedReport() throws Exception {
        var mapper = new ObjectMapper();
        var requestBody = new AtomicReference<String>();
        var status = new AtomicReference<>("completed");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"status\":\"" + status.get() + "\",\"model\":\"test\",\"output\":[{\"type\":\"message\","
                    + "\"content\":[{\"type\":\"output_text\",\"text\":\"Report\"}]}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var properties = new AiOpenAiProperties(); properties.setEnabled(true); properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var client = new OpenAiClient(properties, mapper, mock(AiToolService.class),
                    mock(TrusBotQuoteService.class), mock(TrusBotChantService.class));
            assertEquals("Report", client.generateMatchReport("Instructions", "{}").text());
            var payload = mapper.readTree(requestBody.get());
            assertEquals("Instructions", payload.path("instructions").asText());
            assertEquals("{}", payload.path("input").asText());
            assertFalse(payload.path("store").asBoolean());
            assertFalse(payload.has("tools"));
            status.set("incomplete");
            assertThrows(AiUnavailableException.class, () -> client.generateMatchReport("Instructions", "{}"));
        } finally {
            server.stop(0);
        }
    }
}
