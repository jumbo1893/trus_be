package com.jumbo.trus.dto.notification.push;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnabledPushNotificationDTO {

    private long id;

    private NotificationType type;

    private Boolean enabled;

    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @NotNull
    private Date modificationTime;
}
