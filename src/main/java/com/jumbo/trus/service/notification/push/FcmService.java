package com.jumbo.trus.service.notification.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final GoogleTokenService tokenService;
    private final OkHttpClient client = new OkHttpClient();
    private final DeviceTokenCollector deviceTokenCollector;
    private final ObjectMapper objectMapper;

    public boolean sendPush(DeviceToken deviceToken, String title, String body) {
        try {
            String accessToken = tokenService.getAccessToken();

            Map<String, Object> message = Map.of(
                    "message", Map.of(
                            "token", deviceToken.getToken(),
                            "notification", Map.of(
                                    "title", title,
                                    "body", body
                            ),
                            "data", Map.of(
                                    "title", title,
                                    "body", body
                            ),
                            "apns", Map.of(
                                    "headers", Map.of(
                                            "apns-priority", "10"
                                    ),
                                    "payload", Map.of(
                                            "aps", Map.of(
                                                    "sound", "default"
                                            )
                                    )
                            ),
                            "android", Map.of(
                                    "priority", "HIGH",
                                    "notification", Map.of(
                                            "sound", "default",
                                            "channel_id", "default_channel"
                                    )
                            )
                    )
            );

            String jsonMessage = objectMapper.writeValueAsString(message);

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/v1/projects/trus-flutter/messages:send")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json; UTF-8")
                    .post(RequestBody.create(jsonMessage, MediaType.get("application/json; charset=utf-8")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.warn("FCM failed. status={}, body={}", response.code(), responseBody);

                    if (responseBody.contains("UNREGISTERED")) {
                        deviceTokenCollector.invalidateToken(deviceToken, "EXPIRED");
                        return false;
                    }

                    if (responseBody.contains("INVALID_ARGUMENT")) {
                        deviceTokenCollector.invalidateToken(deviceToken, "INVALID");
                        return false;
                    }

                    return false;
                }

                log.info("FCM sent successfully to deviceTokenId={}, response={}", deviceToken.getId(), responseBody);
                return true;
            }
        } catch (Exception e) {
            log.error("FCM send failed for deviceTokenId={}", deviceToken.getId(), e);
            return false;
        }
    }
}
