package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
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
import java.util.List;
import java.util.Set;
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
            List<PlayerDTO> accomplishedPlayers =
                    getPlayersWhoAccomplishedAchievement(achievement, playerIdList);
            achievementDetail.setAccomplishedPlayers(
                    playerService.getListOfNamesFromListOfPlayers(accomplishedPlayers)
            );
            achievementDetail.setAccomplishedPlayerIds(
                    accomplishedPlayers.stream()
                            .map(PlayerDTO::getId)
                            .collect(Collectors.toSet())
            );
        } else {
            achievementDetail.setAccomplishedPlayerIds(Set.of());
        }

        return achievementDetail;
    }

    public List<Long> getPlayerIdList(long appTeamId) {
        return playerService.convertPlayerListToPlayerIdList(
                playerService.getAll(appTeamId)
        );
    }

    private List<PlayerDTO> getPlayersWhoAccomplishedAchievement(
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

        return accomplishedPlayers;
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
}
