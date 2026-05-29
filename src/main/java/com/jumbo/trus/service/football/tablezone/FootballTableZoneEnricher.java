package com.jumbo.trus.service.football.tablezone;

import com.jumbo.trus.dto.football.TableTeamDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FootballTableZoneEnricher {

    private final FootballTableZoneRuleProvider ruleProvider;

    public List<TableTeamDTO> enrichWithZones(List<TableTeamDTO> tableTeams) {
        if (tableTeams == null || tableTeams.isEmpty()) {
            return tableTeams;
        }

        Optional<FootballTableZoneRules> optionalRules =
                ruleProvider.findRules(tableTeams.get(0).getLeague());

        if (optionalRules.isEmpty()) {
            /*
             * Necháme null.
             * Frontend pozná, že backend pro danou ligu pravidla neposkytl,
             * a může použít svůj fallback.
             */
            return tableTeams;
        }

        FootballTableZoneRules rules = optionalRules.get();
        int tableSize = tableTeams.size();

        tableTeams.forEach(team ->
                team.setTableZone(
                        rules.resolveZone(team.getRank(), tableSize)
                )
        );

        return tableTeams;
    }
}