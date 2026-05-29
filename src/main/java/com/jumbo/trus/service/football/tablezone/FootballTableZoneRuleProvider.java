package com.jumbo.trus.service.football.tablezone;

import com.jumbo.trus.dto.football.FootballTableZone;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.Organization;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FootballTableZoneRuleProvider {

    public Optional<FootballTableZoneRules> findRules(LeagueDTO league) {
        if (league == null || league.getOrganization() != Organization.PKFL) {
            return Optional.empty();
        }

        /*
         * POZOR:
         * Tato varianta platí pro aktuální ročník.
         */
        return switch (league.getRank()) {
            case 1 -> Optional.of(firstLeagueRules());
            case 2 -> Optional.of(secondLeagueRules());
            case 3 -> Optional.of(thirdLeagueRules());
            case 4 -> Optional.of(fourthLeagueRules());
            default -> Optional.empty();
        };
    }

    private FootballTableZoneRules firstLeagueRules() {
        return new FootballTableZoneRules(
                1,
                List.of(
                        FootballTableZoneRule.bottom(
                                2,
                                FootballTableZone.RELEGATION
                        )
                )
        );
    }

    private FootballTableZoneRules secondLeagueRules() {
        return new FootballTableZoneRules(
                2,
                List.of(
                        FootballTableZoneRule.rank(
                                1,
                                FootballTableZone.PROMOTION
                        ),
                        FootballTableZoneRule.range(
                                4,
                                8,
                                FootballTableZone.RELEGATION_PLAYOFF
                        ),
                        FootballTableZoneRule.range(
                                9,
                                10,
                                FootballTableZone.RELEGATION
                        )
                )
        );
    }

    private FootballTableZoneRules thirdLeagueRules() {
        return new FootballTableZoneRules(
                3,
                List.of(
                        FootballTableZoneRule.rank(
                                1,
                                FootballTableZone.PROMOTION
                        ),
                        FootballTableZoneRule.rank(
                                2,
                                FootballTableZone.PROMOTION_PLAYOFF
                        ),
                        FootballTableZoneRule.range(
                                7,
                                12,
                                FootballTableZone.RELEGATION
                        )
                )
        );
    }

    private FootballTableZoneRules fourthLeagueRules() {
        return new FootballTableZoneRules(
                4,
                List.of(
                        FootballTableZoneRule.range(
                                1,
                                2,
                                FootballTableZone.PROMOTION
                        )
                )
        );
    }
}