package com.jumbo.trus.service.match;

import com.jumbo.trus.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchAttendancePercentageService {
    private final MatchRepository matchRepository;

    public Map<Long, Integer> getPercentages(Long appTeamId) {
        Map<Long, Integer> percentages = new LinkedHashMap<>();
        for (MatchRepository.AttendancePercentageRow row :
                matchRepository.findAttendancePercentages(appTeamId, new Date())) {
            long eligible = row.getEligibleMatches();
            long attended = row.getAttendedMatches();
            int percentage = eligible == 0 ? 0
                    : (int) Math.round(100.0 * attended / eligible);
            percentages.put(row.getPlayerId(), Math.max(0, Math.min(100, percentage)));
        }
        return percentages;
    }
}
