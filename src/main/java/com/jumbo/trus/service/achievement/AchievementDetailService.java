package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.order.OrderPlayerByName;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementDetailService {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerAchievementMapper playerAchievementMapper;

    public AchievementDetail getAchievementDetail(long playerAchievementId, AppTeamEntity appTeam) {
        PlayerAchievementDTO playerAchievementDTO = playerAchievementRepository.findById(playerAchievementId)
                .map(playerAchievementMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerAchievementId)));

        AchievementDetail achievementDetail = returnAchievementDetail(
                playerAchievementDTO.getAchievement(),
                getPlayerIdList(appTeam.getId()),
                false
        );

        achievementDetail.setPlayerAchievement(playerAchievementDTO);

        return achievementDetail;
    }

    public AchievementDetail getAchievementDetail(PlayerAchievementDTO playerAchievementDTO, AppTeamEntity appTeam) {
        AchievementDetail achievementDetail = returnAchievementDetail(
                playerAchievementDTO.getAchievement(),
                getPlayerIdList(appTeam.getId()),
                false
        );

        achievementDetail.setPlayerAchievement(playerAchievementDTO);

        return achievementDetail;
    }

    public AchievementDetail returnAchievementDetail(
            AchievementDTO achievement,
            List<Long> playerIdList,
            boolean includeOtherPlayers
    ) {
        AchievementDetail achievementDetail = new AchievementDetail();

        achievementDetail.setAchievement(achievement);

        IMatchIdNumberOneNumberTwo count = getAchievementCount(achievement, playerIdList);

        achievementDetail.setTotalCount(count.getFirstNumber());
        achievementDetail.setAccomplishedCount(count.getSecondNumber());
        achievementDetail.setSuccessRate(calculateSuccessRate(
                count.getFirstNumber(),
                count.getSecondNumber()
        ));

        if (includeOtherPlayers && achievementDetail.getAccomplishedCount() > 0) {
            achievementDetail.setAccomplishedPlayers(
                    getListOfPlayersWhoAccomplishedAchievement(achievement, playerIdList)
            );
        }

        return achievementDetail;
    }

    /**
     * Builds the complete achievement overview from one batched player-achievement
     * query. The previous implementation executed a count query and, for
     * accomplished achievements, another player query for every achievement.
     */
    public List<AchievementDetail> returnAchievementDetails(
            List<AchievementDTO> achievements,
            List<Long> playerIdList
    ) {
        List<PlayerAchievementEntity> playerAchievements = playerIdList.isEmpty()
                ? List.of()
                : playerAchievementRepository.findAllForDetailsByPlayerIds(playerIdList);

        Map<Long, AchievementDetailAccumulator> detailsByAchievement = new HashMap<>();
        for (PlayerAchievementEntity playerAchievement : playerAchievements) {
            if (playerAchievement.getAchievement() == null) {
                continue;
            }

            AchievementDetailAccumulator accumulator = detailsByAchievement.computeIfAbsent(
                    playerAchievement.getAchievement().getId(),
                    ignored -> new AchievementDetailAccumulator()
            );
            accumulator.totalCount++;

            if (Boolean.TRUE.equals(playerAchievement.getAccomplished())) {
                accumulator.accomplishedCount++;
                if (playerAchievement.getPlayer() != null
                        && playerAchievement.getPlayer().getName() != null) {
                    accumulator.accomplishedPlayerNames.add(
                            playerAchievement.getPlayer().getName()
                    );
                }
            }
        }

        return achievements.stream()
                .map(achievement -> createAchievementDetail(
                        achievement,
                        detailsByAchievement.get(achievement.getId())
                ))
                .toList();
    }

    private AchievementDetail createAchievementDetail(
            AchievementDTO achievement,
            AchievementDetailAccumulator accumulator
    ) {
        int totalCount = accumulator == null ? 0 : accumulator.totalCount;
        int accomplishedCount = accumulator == null ? 0 : accumulator.accomplishedCount;

        AchievementDetail detail = new AchievementDetail();
        detail.setAchievement(achievement);
        detail.setTotalCount(totalCount);
        detail.setAccomplishedCount(accomplishedCount);
        detail.setSuccessRate(calculateSuccessRate(totalCount, accomplishedCount));

        if (accumulator != null && !accumulator.accomplishedPlayerNames.isEmpty()) {
            detail.setAccomplishedPlayers(accumulator.accomplishedPlayerNames.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", ")));
        }
        return detail;
    }

    public List<Long> getPlayerIdList(long appTeamId) {
        return playerService.convertPlayerListToPlayerIdList(
                getPlayers(appTeamId)
        );
    }

    public List<PlayerDTO> getPlayers(long appTeamId) {
        return playerService.getAll(appTeamId);
    }

    private String getListOfPlayersWhoAccomplishedAchievement(
            AchievementDTO achievement,
            List<Long> playerIdList
    ) {
        List<PlayerDTO> accomplishedPlayers = new ArrayList<>(
                playerAchievementRepository.findAccomplishedPlayersByAchievement(
                                achievement.getId(),
                                playerIdList
                        )
                        .stream()
                        .map(playerMapper::toDTO)
                        .toList()
        );

        accomplishedPlayers.sort(new OrderPlayerByName());

        return playerService.getListOfNamesFromListOfPlayers(accomplishedPlayers);
    }

    private IMatchIdNumberOneNumberTwo getAchievementCount(
            AchievementDTO achievement,
            List<Long> playerIdList
    ) {
        return playerAchievementRepository.countAchievements(
                playerIdList,
                achievement.getId()
        );
    }

    private float calculateSuccessRate(int totalNumber, int accomplishedNumber) {
        if (totalNumber == 0) {
            return 0F;
        }

        return (float) accomplishedNumber / totalNumber;
    }

    private static final class AchievementDetailAccumulator {
        private int totalCount;
        private int accomplishedCount;
        private final List<String> accomplishedPlayerNames = new ArrayList<>();
    }
}
