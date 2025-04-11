package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity(name = "notification")
@Data
public class NotificationEntity {

    @Id
    @GeneratedValue(generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_seq", allocationSize = 1)
    private Long id;

    private String userName;

    private Date date;

    private String title;

    private String text;

    @ManyToOne
    private AppTeamEntity appTeam;

}
