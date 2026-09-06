package com.jumbo.trus.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ai_match_report_generation")
@Getter
@Setter
public class MatchReportGeneration {
    @Id
    @Column(length = 80)
    private String id;
    @Column(nullable = false, length = 36)
    private String token;
    @Column(nullable = false)
    private Instant expiresAt;
}
