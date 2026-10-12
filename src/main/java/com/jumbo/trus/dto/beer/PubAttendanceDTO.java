package com.jumbo.trus.dto.beer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubAttendanceDTO {
    @NotNull
    private Long matchId;
    private boolean configured;
    @NotNull
    private Set<Long> guestIds;
    @NotNull
    private Set<Long> leftIds;
}
