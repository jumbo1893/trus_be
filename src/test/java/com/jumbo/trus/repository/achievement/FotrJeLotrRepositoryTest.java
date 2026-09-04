package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FotrJeLotrRepositoryTest {

    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");

    @Autowired
    private PlayerAchievementRepository achievementRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private AppTeamRepository appTeamRepository;

    @Test
    void cardCountsOnlyWhenItIsInAMatchAfterFirstChildbirthFine() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Fotr je lotr query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity player = new PlayerEntity();
        player.setName("Testovací otec");
        player.setBirthday(date(1990, 1, 1));
        player.setActive(true);
        player.setAppTeam(appTeam);
        player = playerRepository.saveAndFlush(player);

        FineEntity childbirth = fine("Narození dítěte (holka)", appTeam);
        FineEntity yellowCard = fine("Žlutá karta", appTeam);
        MatchEntity cardBeforeBirth = match("Karta před narozením", date(2024, 1, 1), appTeam);
        MatchEntity birth = match("Narození dítěte", date(2024, 2, 1), appTeam);
        MatchEntity cardAfterBirth = match("Karta po narození", date(2024, 3, 1), appTeam);

        receivedFine(player, yellowCard, cardBeforeBirth, appTeam);
        receivedFine(player, childbirth, birth, appTeam);

        assertThat(achievementRepository.findFotrJeLotr(player.getId(), appTeam.getId()))
                .isNull();

        receivedFine(player, yellowCard, cardAfterBirth, appTeam);

        IMatchIdNumberOneNumberTwo result = achievementRepository.findFotrJeLotr(
                player.getId(), appTeam.getId());
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo(cardAfterBirth.getId());
        assertThat(result.getFirstNumber()).isEqualTo(1);
        assertThat(result.getSecondNumber()).isEqualTo(2);
    }

    private FineEntity fine(String name, AppTeamEntity appTeam) {
        FineEntity fine = new FineEntity();
        fine.setName(name);
        fine.setAmount(100);
        fine.setAppTeam(appTeam);
        return fineRepository.saveAndFlush(fine);
    }

    private MatchEntity match(String name, Date date, AppTeamEntity appTeam) {
        MatchEntity match = new MatchEntity();
        match.setName(name);
        match.setDate(date);
        match.setAppTeam(appTeam);
        return matchRepository.saveAndFlush(match);
    }

    private void receivedFine(
            PlayerEntity player,
            FineEntity fine,
            MatchEntity match,
            AppTeamEntity appTeam
    ) {
        ReceivedFineEntity receivedFine = new ReceivedFineEntity();
        receivedFine.setPlayer(player);
        receivedFine.setFine(fine);
        receivedFine.setMatch(match);
        receivedFine.setAppTeam(appTeam);
        receivedFine.setFineNumber(1);
        receivedFineRepository.saveAndFlush(receivedFine);
    }

    private static Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day)
                .atStartOfDay(PRAGUE)
                .toInstant());
    }
}
