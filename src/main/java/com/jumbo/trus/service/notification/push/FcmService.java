package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.entity.notification.push.DeviceToken;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final GoogleTokenService tokenService;
    private final OkHttpClient client = new OkHttpClient();

    public void sendPush(DeviceToken deviceToken, String title, String body) throws Exception {
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
                assert response.body() != null;
                throw new RuntimeException("FCM error: " + response.code() + " " + response.body().string());
            }
        }
    }

}
