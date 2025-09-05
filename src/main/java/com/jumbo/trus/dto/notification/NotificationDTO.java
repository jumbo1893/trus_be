package com.jumbo.trus.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {

    private long id;

    private String userName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @NotNull
    private Date date;

    private String title;

    private String text;
}
