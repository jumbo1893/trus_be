package com.jumbo.trus.dto.membership;

import com.jumbo.trus.entity.membership.MembershipTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class MembershipGrantRequest {

    @NotNull
    private MembershipTier tier;

    /** True znamená členství bez konce. */
    private Boolean unlimited = false;

    /** Lze kombinovat s durationHours, například 25 dní a 8 hodin. */
    @Min(0)
    private Long durationDays;

    @Min(0)
    private Long durationHours;

    /** Alternativa k durationDays/durationHours. Musí ležet v budoucnosti. */
    private Instant validUntil;
}
