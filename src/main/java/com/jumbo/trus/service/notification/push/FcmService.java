package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.entity.notification.push.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final GoogleTokenService tokenService;
    private final OkHttpClient client = new OkHttpClient();
    private final DeviceTokenCollector deviceTokenCollector;

    public boolean sendPush(DeviceToken deviceToken, String title, String body) throws Exception {
        String accessToken = tokenService.getAccessToken();

        String jsonMessage = """
    {
      "message": {
        "token": "%s",
        "notification": {
          "title": "%s",
          "body": "%s"
        },
        "data": {
          "title": "%s",
          "body": "%s"
        }
      }
    }
    """.formatted(deviceToken.getToken(), title, body, title, body);

        Request request = new Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/trus-flutter/messages:send")
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(RequestBody.create(jsonMessage, MediaType.get("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.code() == 404 && responseBody.contains("UNREGISTERED")) {
                    // Token už není platný
                    deviceTokenCollector.invalidateToken(deviceToken, "EXPIRED");
                    return false;
                }
                else if (response.code() == 400 && responseBody.contains("INVALID_ARGUMENT")) {
                    // Token už není platný
                    deviceTokenCollector.invalidateToken(deviceToken, "INVALID");
                    return false;
                }
                else {
                    throw new RuntimeException("FCM error: " + response.code() + " " + responseBody);
                }
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

}
