package com.jumbo.trus.entity.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.dto.log.LogDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "log")
@Data
@NoArgsConstructor
public class LogEntity {

    @Id
    @GeneratedValue(generator = "log_seq")
    @SequenceGenerator(name = "log_seq", sequenceName = "log_seq", allocationSize = 1)
    private Long id;

    private String message;

    private String logClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date time;

    private String deviceType;

    public LogEntity(LogDTO logDTO, UserEntity user, String deviceType) {
        this.message = logDTO.getMessage();
        this.logClass = logDTO.getLogClass();
        this.time = new Date();
        this.user = user;
        this.deviceType = deviceType;
    }


}
