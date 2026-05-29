package com.jumbo.trus.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDetailedResponse {

    private int playersCount = 0;

    private int matchesCount = 0;

    @NotNull
    private List<AttendanceDetailedDTO> attendanceList;

}