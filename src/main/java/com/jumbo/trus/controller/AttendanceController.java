package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.attendance.AttendanceDetailedResponse;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AppTeamService appTeamService;

    @RoleRequired("READER")
    @GetMapping("/get-all-detailed")
    public AttendanceDetailedResponse getDetailedAttendance(StatisticsFilter filter) {
        filter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return attendanceService.getAllDetailed(filter);
    }
}