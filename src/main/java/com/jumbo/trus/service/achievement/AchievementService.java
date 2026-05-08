package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.lock.LockManager;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.order.OrderAchievementBySuccessRate;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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
    private final LockManager lockManager;
    private final AchievementDetailService achievementDetailService;

    @Async
    @Transactional
    public void updateAllPlayerAchievements(AppTeamEntity appTeam, AchievementType achievementType) {
        if (appTeam == null) {
            throw new IllegalStateException("AppTeamId nebyl nastaven!");
        }
        ReentrantLock lock = lockManager.getLock(appTeam.getId());
        if (lock.tryLock()) { // Non-blocking pokus – pokud zamčené, počkáme (fair lock)
            try {
                log.debug("Začínám počítat pro appTeam {}!", appTeam.getId());
                achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
            } finally {
                lock.unlock();
            }
        } else {
            // Pokud nelze získat lock ihned, zařaď do fronty nebo retry – ale pro jednoduchost: čekej blokujícím způsobem
            lock.lock(); // Blokující čekání
            try {
                achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
            } finally {
                lock.unlock();
            }
        }
    }
        /*achievementCalculator.calculateAllAchievements(playerService.getAll(appTeam.getId()), appTeam, achievementType);
        log.debug("Vypočteno!");*/


    public void updatePlayerAchievements(Long playerId, AppTeamEntity appTeam) {
        List<PlayerDTO> playerDTOList = new ArrayList<>();
        playerDTOList.add(playerService.getPlayer(playerId));
        achievementCalculator.calculateAllAchievements(playerDTOList, appTeam, AchievementType.ALL);
    }

    public AchievementDetail getAchievementDetail(long playerAchievementId, AppTeamEntity appTeam) {
        return achievementDetailService.getAchievementDetail(playerAchievementId, appTeam);
    }

    public PlayerAchievementDTO editPlayerAchievement(Long oldAchievementId, PlayerAchievementDTO playerAchievementDTO, AppTeamEntity appTeam) throws NotFoundException {
        PlayerAchievementDTO oldAchievement = playerAchievementRepository.findById(oldAchievementId).map(playerAchievementMapper::toDTO).orElseThrow(() -> new EntityNotFoundException(String.valueOf(oldAchievementId)));
        achievementCalculator.updateExistingAchievement(oldAchievement, playerAchievementDTO, appTeam);
        return playerAchievementDTO;
    }


    public List<AchievementDetail> getAllDetailedAchievements(long appTeamId) {
        List<AchievementDetail> achievementDetailList = new ArrayList<>();

        List<Long> playerIdList = achievementDetailService.getPlayerIdList(appTeamId);

        List<AchievementDTO> achievements = achievementRepository.findAll()
                .stream()
                .map(achievementMapper::toDTO)
                .toList();

        for (AchievementDTO achievement : achievements) {
            achievementDetailList.add(
                    achievementDetailService.returnAchievementDetail(
                            achievement,
                            playerIdList,
                            true
                    )
            );
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

    private float calculateSuccessRate(int totalNumber, int accomplishedNumber) {
        if (totalNumber == 0) {
            return 0F;
        }
        return (float) accomplishedNumber/totalNumber;
    }
}