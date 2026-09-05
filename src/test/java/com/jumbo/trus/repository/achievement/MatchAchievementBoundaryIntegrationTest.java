package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.service.achievement.AchievementCodes;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchAchievementBoundaryIntegrationTest extends AchievementDatabaseFixture {
    @ParameterizedTest
    @CsvSource({
        "KAZDEMU_CO_MU_PATRI,1,1,1,1,true", "KAZDEMU_CO_MU_PATRI,1,0,1,0,false",
        "KAZDEMU_CO_MU_PATRI,2,1,1,1,false", "KAZDEMU_CO_MU_PATRI,0,0,0,0,false",
        "KORALA,0,1,0,0,true", "KORALA,1,2,0,0,true", "KORALA,1,1,0,0,false", "KORALA,2,1,0,0,false",
        "KOMPLEXNI_HRAC,0,0,1,1,true", "KOMPLEXNI_HRAC,0,0,1,0,false", "KOMPLEXNI_HRAC,0,0,0,1,false",
        "NESOBECKY_HRDINA,0,0,0,2,false", "NESOBECKY_HRDINA,0,0,0,3,true", "NESOBECKY_HRDINA,0,0,0,4,true"
    })
    void drinkAndPointBoundaries(String code, int beers, int shots, int goals, int assists, boolean expected) {
        beer(player, match, beers, shots); goal(player, match, goals, assists);
        assertMatch(code, expected);
    }

    @ParameterizedTest
    @CsvSource({"2,1,2,0,true", "2,1,2,1,false", "2,1,3,1,false", "0,0,0,0,false"})
    void oslavenecMustBeatTheCombinedRestOfTheTeam(int beers, int shots, int otherBeers, int otherShots, boolean expected) {
        beer(player, match, beers, shots); beer(teammate, match, otherBeers, otherShots);
        assertMatch(AchievementCodes.OSLAVENEC, expected);
    }

    @ParameterizedTest
    @CsvSource({
        "TEN_TO_PERFEKTNE_KOPE,MISSED_PENALTY,0,false", "TEN_TO_PERFEKTNE_KOPE,MISSED_PENALTY,1,true",
        "TEN_TO_PERFEKTNE_KOPE,OVERKICK,1,false",
        "ALZHEIMER,FORGOTTEN_THINGS,1,true", "ALZHEIMER,INCOMPLETE_EQUIPMENT,1,true",
        "ALZHEIMER,FORGOTTEN_THINGS,0,false", "ALZHEIMER,NEW_BOOTS,1,false",
        "LEO_BERANEK,NEW_BOOTS,1,true", "LEO_BERANEK,NEW_BOOTS,0,false", "LEO_BERANEK,OVERKICK,1,false"
    })
    void individualFineBoundaries(String code, String fineCode, int count, boolean expected) {
        fine(player, match, fineCode, count);
        // Positive data belonging to somebody else must not help this player.
        fine(teammate, match, FineCodes.MISSED_PENALTY, 10);
        assertMatch(code, expected);
    }

    @ParameterizedTest
    @CsvSource({
        "ZLUTY_HNEDY_POPLACH,HANGOVER,BATHROOM_DURING_MATCH,1,1,true",
        "ZLUTY_HNEDY_POPLACH,HANGOVER,BATHROOM_DURING_MATCH,0,1,false",
        "ZLUTY_HNEDY_POPLACH,HANGOVER,BATHROOM_DURING_MATCH,1,0,false",
        "PROC,HANGOVER,YELLOW_CARD,1,1,true", "PROC,HANGOVER,RED_CARD,1,1,true",
        "PROC,HANGOVER,YELLOW_CARD,0,1,false", "PROC,HANGOVER,YELLOW_CARD,1,0,false",
        "DLOUHA_NOC,HANGOVER,LATE_AFTER_START,1,1,true", "DLOUHA_NOC,HANGOVER,LATE_AFTER_START,1,0,false",
        "DLOUHA_NOC,HANGOVER,LATE_AFTER_START,0,1,false",
        "JEN_NA_SKOK,LATE_AFTER_START,RED_CARD,1,1,true", "JEN_NA_SKOK,LATE_AFTER_START,YELLOW_CARD,1,1,false",
        "JEN_NA_SKOK,LATE_AFTER_START,RED_CARD,0,1,false",
        "FLAKAC,LATE_AFTER_START,THIRD_HALF,1,1,true", "FLAKAC,LATE_AFTER_START,THIRD_HALF,1,0,false",
        "FLAKAC,LATE_AFTER_START,THIRD_HALF,0,1,false"
    })
    void fineConjunctionNeedsBothFacts(String code, String first, String second, int firstCount, int secondCount, boolean expected) {
        fine(player, match, first, firstCount); fine(player, match, second, secondCount);
        assertMatch(code, expected);
    }

    @ParameterizedTest
    @CsvSource({
        "IONTAK,THIRD_HALF,1,0,1,true", "IONTAK,THIRD_HALF,0,1,1,false", "IONTAK,THIRD_HALF,1,0,0,false",
        "HLADINKA,HANGOVER,0,1,1,true", "HLADINKA,HANGOVER,1,0,1,false", "HLADINKA,HANGOVER,0,1,0,false",
        "OZEN_SE_OZER_SE,WEDDING,4,4,1,true", "OZEN_SE_OZER_SE,WEDDING,4,3,1,false",
        "OZEN_SE_OZER_SE,WEDDING,8,0,0,false", "OZEN_SE_OZER_SE,WEDDING,0,8,1,true"
    })
    void drinksNeedTheCorrectFineInTheSameMatch(String code, String fineCode, int beers, int shots, int count, boolean expected) {
        beer(player, match, beers, shots); fine(player, match, fineCode, count);
        assertMatch(code, expected);
    }

    @ParameterizedTest
    @CsvSource({"1,1,YELLOW_CARD,1,true", "1,1,RED_CARD,1,true", "0,1,YELLOW_CARD,1,false",
        "1,0,YELLOW_CARD,1,false", "1,1,YELLOW_CARD,0,false", "1,1,OVERKICK,1,false"})
    void gordieHoweNeedsGoalAssistAndEitherCard(int goals, int assists, String code, int cards, boolean expected) {
        goal(player, match, goals, assists); fine(player, match, code, cards);
        assertMatch(AchievementCodes.HATTRICK_GORDIEHO_HOWA, expected);
    }

    @ParameterizedTest
    @CsvSource({"2,3,false", "3,2,false", "3,3,true", "4,3,true", "3,0,false"})
    void sharedScorerRequiresTwoHattrickScorers(int goals, int teammateGoals, boolean expected) {
        goal(player, match, goals, 0); goal(teammate, match, teammateGoals, 0);
        assertMatch(AchievementCodes.SDILENY_STRELEC, expected);
    }

    @ParameterizedTest
    @CsvSource({"true,0,true", "true,1,false", "false,0,false"})
    void cernaPraceRequiresStarAndNoGoal(boolean best, int goals, boolean expected) {
        performance(player, match, 0, best, false); goal(player, match, goals, 0);
        assertMatch(AchievementCodes.CERNA_PRACE, expected);
    }
}
