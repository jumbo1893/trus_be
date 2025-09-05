package com.jumbo.trus.entity.notification.push;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "device_token")
@Data
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(generator = "device_token_seq")
    @SequenceGenerator(name = "device_token_seq", sequenceName = "device_token_seq", allocationSize = 1)
    private Long id;

    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date registrationTime;

    private String deviceType;

    @OneToMany(mappedBy = "deviceToken", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentPushNotification> sentPushNotification;

    public DeviceToken(String token, UserEntity user, Date registrationTime, String deviceType) {
        this.token = token;
        this.user = user;
        this.registrationTime = registrationTime;
        this.deviceType = deviceType;
    }
}
