package com.jumbo.trus.repository;

import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.fine.FineCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReceivedFineRepositoryCodeTest {

    @Autowired private AppTeamRepository appTeamRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private ReceivedFineRepository receivedFineRepository;

    @Test
    void codeQueriesAndAutomaticCleanupCoverEveryAmountVersion() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Fine code test " + UUID.randomUUID());
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity player = new PlayerEntity();
        player.setName("Testovací hráč");
        player.setBirthday(new Date(0));
        player.setActive(true);
        player.setAppTeam(appTeam);
        player = playerRepository.saveAndFlush(player);

        MatchEntity match = new MatchEntity();
        match.setName("Test verzí pokuty");
        match.setAppTeam(appTeam);
        match = matchRepository.saveAndFlush(match);

        FineEntity historical = fine(
                "Historická prohra", FineCodes.LOSS_PLAYING, 20, true, appTeam);
        FineEntity current = fine(
                "Aktuální prohra", FineCodes.LOSS_PLAYING, 50, false, appTeam);

        receivedFine(player, match, historical, appTeam);
        receivedFine(player, match, current, appTeam);

        assertThat(receivedFineRepository.findAllByPlayerIdFineCodeAndMatchesId(
                player.getId(), FineCodes.LOSS_PLAYING, List.of(match.getId())))
                .hasSize(2);

        receivedFineRepository.deleteAutomaticResultFinesFromMatch(
                match.getId(), appTeam.getId(), List.of(FineCodes.LOSS_PLAYING));
        receivedFineRepository.flush();

        assertThat(receivedFineRepository.findAllByPlayerIdFineCodeAndMatchesId(
                player.getId(), FineCodes.LOSS_PLAYING, List.of(match.getId())))
                .isEmpty();
    }

    private FineEntity fine(
            String name,
            String code,
            int amount,
            boolean inactive,
            AppTeamEntity appTeam
    ) {
        FineEntity fine = new FineEntity();
        fine.setName(name);
        fine.setCode(code);
        fine.setAmount(amount);
        fine.setEditable(false);
        fine.setInactive(inactive);
        fine.setAppTeam(appTeam);
        return fineRepository.saveAndFlush(fine);
    }

    private void receivedFine(
            PlayerEntity player,
            MatchEntity match,
            FineEntity fine,
            AppTeamEntity appTeam
    ) {
        ReceivedFineEntity receivedFine = new ReceivedFineEntity();
        receivedFine.setFineNumber(1);
        receivedFine.setPlayer(player);
        receivedFine.setMatch(match);
        receivedFine.setFine(fine);
        receivedFine.setAppTeam(appTeam);
        receivedFineRepository.saveAndFlush(receivedFine);
    }
}
