package com.jumbo.trus.dto.notification.push;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceTokenDTO {

    private Long id;

    private String token;

    private Long userId;

    private String deviceType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date registrationTime;

    public DeviceTokenDTO(DeviceToken entity) {
        this.id = entity.getId();
        this.token = entity.getToken();
        this.registrationTime = entity.getRegistrationTime();
        this.deviceType = entity.getDeviceType();
        this.userId = entity.getUser() != null ? entity.getUser().getId() : null;
    }

    public static DeviceTokenDTO fromEntity(DeviceToken entity) {
        return new DeviceTokenDTO(entity);
    }

}
