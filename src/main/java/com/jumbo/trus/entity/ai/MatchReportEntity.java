package com.jumbo.trus.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ai_match_report", indexes = @Index(name = "idx_match_report_lookup",
        columnList = "appTeamId,footballMatchId,style,id"))
@Getter
@Setter
public class MatchReportEntity {
    @Id
    @GeneratedValue(generator = "ai_match_report_seq")
    @SequenceGenerator(name = "ai_match_report_seq", sequenceName = "ai_match_report_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false)
    private Long appTeamId;
    @Column(nullable = false)
    private Long footballMatchId;
    @Column(nullable = false)
    private Long generatedByUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchReportStyle style;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
    @Column(nullable = false)
    private Instant generatedAt;
}
