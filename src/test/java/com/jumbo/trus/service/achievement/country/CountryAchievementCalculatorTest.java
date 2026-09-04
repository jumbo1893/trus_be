package com.jumbo.trus.service.achievement.country;

import com.jumbo.trus.dto.VisitedCountryResponse;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.AchievementCodes;
import com.jumbo.trus.service.achievement.helper.AchievementEligibilityService;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CountryAchievementCalculatorTest {

    private static final List<String> COUNTRY_CODES = List.of(
            AchievementCodes.ZAHRANICNI_POZOROVATEL,
            AchievementCodes.DO_AFRIKY_NA_CERNOSKY,
            AchievementCodes.HEDVABNA_STEZKA,
            AchievementCodes.AMERICAN_Z_VYSOCAN,
            AchievementCodes.PO_STOPACH_DIEGA,
            AchievementCodes.TRUSI_AMUNDSEN,
            AchievementCodes.LISAK_A_MORE
    );

    @Mock private AchievementRepository achievementRepository;
    @Mock private PlayerAchievementRepository playerAchievementRepository;
    @Mock private PlayerMapper playerMapper;
    @Mock private AchievementNotificationMaker achievementNotificationMaker;
    @Mock private MembershipService membershipService;
    @Mock private PlayerAchievementMapper playerAchievementMapper;
    @Mock private AchievementEligibilityService achievementEligibilityService;

    @InjectMocks private CountryAchievementCalculator calculator;

    static Stream<Arguments> countryRules() {
        return Stream.of(
                Arguments.of("CZ", "Česko", "EU", Set.of()),
                Arguments.of("DE", "Německo", "EU", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL)),
                Arguments.of("ZA", "Jihoafrická republika", "AF", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.DO_AFRIKY_NA_CERNOSKY)),
                Arguments.of("JP", "Japonsko", "AS", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.HEDVABNA_STEZKA)),
                Arguments.of("US", "Spojené státy", "NA", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.AMERICAN_Z_VYSOCAN)),
                Arguments.of("AR", "Argentina", "SA", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.PO_STOPACH_DIEGA)),
                Arguments.of("AQ", "Antarktida", "AN", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.TRUSI_AMUNDSEN)),
                Arguments.of("AU", "Austrálie", "OC", Set.of(AchievementCodes.ZAHRANICNI_POZOROVATEL, AchievementCodes.LISAK_A_MORE))
        );
    }

    @ParameterizedTest(name = "{0} splní {3}")
    @MethodSource("countryRules")
    void awardsExactlyTheAchievementsDescribedForCountry(
            String countryCode,
            String countryName,
            String continentCode,
            Set<String> expectedAccomplishedCodes
    ) {
        PlayerDTO player = new PlayerDTO();
        player.setId(7L);
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(7L);
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(11L);
        List<AchievementEntity> achievements = achievements();

        when(achievementRepository.findAllByCodeIn(any())).thenReturn(achievements);
        when(playerMapper.toEntity(player)).thenReturn(playerEntity);
        when(achievementEligibilityService.canHaveAchievement(any(), any())).thenReturn(true);
        when(playerAchievementRepository.findAllByPlayerIdAndAchievementIdIn(any(), anyList()))
                .thenReturn(List.of());
        when(playerAchievementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(playerAchievementMapper.toDTO(any())).thenReturn(new PlayerAchievementDTO());

        calculator.calculateCountryAchievementsByPlayer(
                player,
                new VisitedCountryResponse(countryCode, countryName, LocalDateTime.now(), continentCode),
                appTeam
        );

        ArgumentCaptor<List<PlayerAchievementEntity>> savedCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(playerAchievementRepository).saveAll(savedCaptor.capture());
        List<PlayerAchievementEntity> saved = savedCaptor.getValue();
        assertThat(saved).hasSize(COUNTRY_CODES.size());
        assertThat(saved.stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getAccomplished()))
                .map(entity -> entity.getAchievement().getCode()))
                .containsExactlyInAnyOrderElementsOf(expectedAccomplishedCodes);
        assertThat(saved.stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getAccomplished()))
                .map(PlayerAchievementEntity::getDetail))
                .allSatisfy(detail -> assertThat(detail).contains(countryName));
    }

    private List<AchievementEntity> achievements() {
        AtomicLong id = new AtomicLong(1);
        return COUNTRY_CODES.stream().map(code -> {
            AchievementEntity achievement = new AchievementEntity();
            achievement.setId(id.getAndIncrement());
            achievement.setCode(code);
            achievement.setOnlyForPlayers(false);
            return achievement;
        }).toList();
    }
}
