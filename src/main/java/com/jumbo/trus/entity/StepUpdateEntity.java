package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "step_update", uniqueConstraints = {
        @UniqueConstraint(name = "uk_step_update_user_date", columnNames = {"user_id", "step_date"})
})
@Data
public class StepUpdateEntity {

    @Id
    @GeneratedValue(generator = "step_update_seq")
    @SequenceGenerator(name = "step_update_seq", sequenceName = "step_update_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "step_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepSource source;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private OffsetDateTime measuredUntil;

    @Column(nullable = false)
    private Instant updateTime;

}
