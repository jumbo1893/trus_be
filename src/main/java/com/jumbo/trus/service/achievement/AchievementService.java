package com.jumbo.trus.service.achievement;

import com.jumbo.trus.aspect.appteam.AppTeamContextHolder;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.repository.achievement.AchievementRepository;
import com.jumbo.trus.entity.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.order.OrderAchievementBySuccessRate;
import com.jumbo.trus.service.order.OrderPlayerByName;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final PlayerService playerService;
    private final AchievementMapper achievementMapper;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final AchievementCalculator achievementCalculator;
    private final PlayerMapper playerMapper;

    @Async
    @Transactional
    public void updateAllPlayerAchievements(AppTeamEntity appTeam, AchievementType achievementType) {

        if (appTeam == null) {
            throw new IllegalStateException("AppTeamId nebyl nastaven!");
        }

        achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
        log.debug("Vypočteno!");
    }

    public AchievementDetail getAchievementDetail(long playerAchievementId, AppTeamEntity appTeam) {
        PlayerAchievementDTO playerAchievementDTO = playerAchievementRepository.findById(playerAchievementId).map(playerAchievementMapper::toDTO).orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerAchievementId)));
        AchievementDetail achievementDetail = returnAchievementDetail(playerAchievementDTO.getAchievement(), getPlayerIdList(appTeam.getId()), false);
        achievementDetail.setPlayerAchievement(playerAchievementDTO);
        return achievementDetail;
    }

    public PlayerAchievementDTO editPlayerAchievement(Long playerAchievementId, PlayerAchievementDTO playerAchievementDTO) throws NotFoundException {
        PlayerAchievementEntity foundEntity = playerAchievementRepository.findById(playerAchievementId)
                .orElseThrow(() -> new NotFoundException("Achievement s id " + playerAchievementId + "nenalezen v db"));
        PlayerAchievementEntity entity = playerAchievementMapper.toEntity(playerAchievementDTO);
        entity.setId(playerAchievementId);
        PlayerAchievementEntity savedEntity = playerAchievementRepository.save(entity);
        return playerAchievementMapper.toDTO(savedEntity);
    }


    public List<AchievementDetail> getAllDetailedAchievements(long appTeamId) {
        List<AchievementDetail> achievementDetailList = new ArrayList<>();
        List<Long> playerIdList = getPlayerIdList(appTeamId);
        List<AchievementDTO> achievements = achievementRepository.findAll().stream().map(achievementMapper::toDTO).toList();
        for (AchievementDTO achievement : achievements) {
            achievementDetailList.add(returnAchievementDetail(achievement, playerIdList, true));
        }
        achievementDetailList.sort(new OrderAchievementBySuccessRate());
        return achievementDetailList;
    }

    public AchievementPlayerDetail getAchievementsForPlayer(Long playerId) {
        List<PlayerAchievementDTO> playerAchievementList = playerAchievementRepository.findAllByPlayerId(playerId).stream().map(playerAchievementMapper::toDTO).toList();
        AchievementPlayerDetail achievementPlayerDetail = new AchievementPlayerDetail();
        for (PlayerAchievementDTO playerAchievementDTO : playerAchievementList) {
            if (playerAchievementDTO.getAccomplished().equals(true)) {
                achievementPlayerDetail.getAccomplishedPlayerAchievements().add(playerAchievementDTO);
            }
            else {
                achievementPlayerDetail.getNotAccomplishedPlayerAchievements().add(playerAchievementDTO);
            }
        }
        achievementPlayerDetail.setTotalCount(playerAchievementList.size());
        achievementPlayerDetail.setSuccessRate(calculateSuccessRate(playerAchievementList.size(), achievementPlayerDetail.getAccomplishedPlayerAchievements().size()));
        return achievementPlayerDetail;
    }

    private List<Long> getPlayerIdList(long appTeamId) {
        return playerService.convertPlayerListToPlayerIdList(playerService.getAll(appTeamId));
    }

    private AchievementDetail returnAchievementDetail(AchievementDTO achievement, List<Long> playerIdList, boolean includeOtherPlayers) {
        AchievementDetail achievementDetail = new AchievementDetail();
        achievementDetail.setAchievement(achievement);
        IMatchIdNumberOneNumberTwo count = getAchievementCount(achievement, playerIdList);
        achievementDetail.setTotalCount(count.getFirstNumber());
        achievementDetail.setAccomplishedCount(count.getSecondNumber());
        achievementDetail.setSuccessRate(calculateSuccessRate(count.getFirstNumber(), count.getSecondNumber()));
        if (includeOtherPlayers && achievementDetail.getAccomplishedCount() > 0) {
            achievementDetail.setAccomplishedPlayers(getListOfPlayersWhoAccomplishedAchievement(achievement, playerIdList));
        }
        return achievementDetail;
    }

    private String getListOfPlayersWhoAccomplishedAchievement(AchievementDTO achievement, List<Long> playerIdList) {
        List<PlayerDTO> accomplishedPlayers = new ArrayList<>(playerAchievementRepository.findAccomplishedPlayersByAchievement(achievement.getId(), playerIdList).stream().map(playerMapper::toDTO).toList());
        accomplishedPlayers.sort(new OrderPlayerByName());
        return playerService.getListOfNamesFromListOfPlayers(accomplishedPlayers);
    }

    private IMatchIdNumberOneNumberTwo getAchievementCount(AchievementDTO achievement, List<Long> playerIdList) {
        return playerAchievementRepository.countAchievements(playerIdList, achievement.getId());
    }

    private float calculateSuccessRate(int totalNumber, int accomplishedNumber) {
        if (totalNumber == 0) {
            return 0F;
        }
        return (float) accomplishedNumber/totalNumber;
    }

}