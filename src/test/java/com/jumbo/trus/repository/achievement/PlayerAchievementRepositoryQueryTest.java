package com.jumbo.trus.repository.achievement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class PlayerAchievementRepositoryQueryTest {

    @Autowired
    private PlayerAchievementRepository repository;

    @Test
    void eventScopedQueriesAreValidPostgresSql() {
        long missingId = -1L;

        assertDoesNotThrow(() -> {
            repository.findJardaKuzel(missingId, missingId, missingId);
            repository.findKlubSracu(missingId, missingId, missingId);
            repository.findOsamelyDrzak(missingId, missingId, missingId);
            repository.findVeDvouSeToLepeTahne(missingId, missingId, missingId);
            repository.findNastupJakoHromGoal(missingId, missingId, missingId);
            repository.findMachyrek(missingId, missingId, missingId);
            repository.findFirstThreeConsecutiveMatchesWithGoal(missingId, missingId, missingId);
            repository.findDoPoctu(missingId, missingId, missingId);
            repository.findPlayerAndTotalDrinksInMatch(missingId, missingId);
            repository.findBestPlayerWithFineInMatch(missingId, missingId, List.of("test"));
            repository.findWinningMatchWithFine(missingId, missingId, "test", missingId);
            repository.findMatchWherePlayerReceivedAtLeastXFines(missingId, missingId);
            repository.findAccomplishedByPlayerAndMatch(missingId, missingId);
            repository.findRobertoCarlosInMatch(missingId, missingId);
            repository.findSpilmachrInMatch(missingId, missingId);
            repository.findJaToZaVasObehalInMatch(missingId, missingId);
            repository.findDoplneniTekutinInMatch(missingId, missingId);
            repository.findCerneGenyInMatch(missingId, missingId);
            repository.findSdilenyStrelecInMatch(missingId, missingId);
            repository.findNesobeckyHrdinaInMatch(missingId, missingId);
            repository.findModerniGolmanskaSkolaInMatch(missingId, missingId);
            repository.findMoralniPodporaInMatch(missingId, missingId);
            repository.findHattrickGordiehoHowaInMatch(missingId, missingId);
            repository.findCernaPraceInMatch(missingId, missingId);
            repository.findAutickoInMatch(missingId, missingId);
            repository.findRossGellerAtMatch(missingId, missingId);
            repository.findFirstAttendanceIfMatch(missingId, missingId);
            repository.findCirhozaAtMatch(missingId, missingId, missingId);
            repository.findTahounAtMatch(missingId, missingId, missingId);
            repository.getMatchWithGoalYellowBeerAndLiquor(missingId, "test", missingId);
            repository.findTeamPlayerInSeason(missingId, missingId, missingId);
            repository.findMaloCasuHodneMuziky(missingId, missingId, missingId);
        });
    }
}
