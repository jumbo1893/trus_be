package com.jumbo.trus.service.football.tablezone;

import com.jumbo.trus.dto.football.FootballTableZone;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class FootballTableZoneRules {

    private final int leagueRank;
    private final List<FootballTableZoneRule> rules;

    public FootballTableZone resolveZone(int teamRank, int tableSize) {
        return rules.stream()
                .filter(rule -> rule.appliesTo(teamRank, tableSize))
                .map(FootballTableZoneRule::getZone)
                .findFirst()
                .orElse(FootballTableZone.NEUTRAL);
    }
}