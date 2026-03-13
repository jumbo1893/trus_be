package com.jumbo.trus.service.activity.footbar;

import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.dto.footbar.profile.FootbarProfile;
import com.jumbo.trus.dto.footbar.response.FootbarAccountSessions;
import com.jumbo.trus.dto.footbar.response.FootbarSessionSetup;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.repository.footbar.FootbarAccountRepository;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.activity.footbar.connect.FootbarConnect;
import com.jumbo.trus.service.activity.footbar.profile.FootbarProfileProcessor;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionGetter;
import com.jumbo.trus.service.activity.footbar.session.FootbarSessionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FootbarService {

    private final FootbarAccountRepository footbarAccountRepository;
    private final FootbarConnect footbarConnect;
    private final FootbarSessionProcessor footbarSessionProcessor;
    private final FootbarProfileProcessor footbarProfileProcessor;
    private final UpdateService updateService;
    private final FootbarSessionGetter footbarSessionGetter;
    private final MatchService matchService;
    private final MatchMapper matchMapper;
    private final SeasonService seasonService;

    private final static String FOOTBAR_SESSION_UPDATE = "footbar_session_update";

    public void exchangeCodeForToken(String code) {
        footbarConnect.exchangeCodeForToken(code);
    }

    public String connectRedirectUrl() {
        return footbarConnect.connectRedirectUrl();
    }

    public void syncSession(Long accountId, AppTeamEntity appTeam) {
        FootbarAccountEntity account = footbarAccountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("account not found"));
        footbarSessionProcessor.saveSessions(account, appTeam);
    }

    @Transactional
    public Date syncSessions(AppTeamEntity appTeam) {
        List<FootbarAccountEntity> accountEntities = footbarAccountRepository.findAllAccountsByAppTeam(appTeam);
        for (FootbarAccountEntity account : accountEntities) {
            footbarSessionProcessor.saveSessions(account, appTeam);
        }
        return updateService.saveNewUniqueUpdate(FOOTBAR_SESSION_UPDATE, appTeam.getId()).getDate();
    }

    public Date getLastSyncDate(AppTeamEntity appTeam) {
        UpdateDTO updateDTO = updateService.getUpdateByNameAndAppTeamId(FOOTBAR_SESSION_UPDATE, appTeam.getId());
        if (updateDTO == null) {
            return null;
        } else {
            return updateDTO.getDate();
        }
    }

    public FootbarProfile getFootbalProfile(Long userId) {
        FootbarAccountEntity footbarAccount = footbarAccountRepository.findByUserId(userId).orElse(null);
        if (footbarAccount != null) {
            return footbarProfileProcessor.fetchFootbarProfileDetail(footbarAccount.getFootbarUserId(), footbarConnect.getValidAccessToken(footbarAccount));
        }
        FootbarProfile inactiveProfile = new FootbarProfile();
        inactiveProfile.setActive(false);
        return inactiveProfile;
    }

    public FootbarSessionSetup getFootbarSessionCompareSetup(Long seasonId, AppTeamEntity appTeam, Long userId) {
        List<MatchDTO> matchDTOList = new ArrayList<>();
        List<FootbarAccountSessions> footbarAccountSessions = new ArrayList<>();
        List<MatchEntity> matchEntities = matchService.getAllEntitiesBySeasonId(appTeam, seasonId);
        for (MatchEntity match : matchEntities) {
            FootbarAccountSessions newSession = footbarSessionGetter.getFootbarAccountSessionByMatch(appTeam, match, userId);
            if (newSession != null) {
                footbarAccountSessions.add(newSession);
                matchDTOList.add(matchMapper.toDTO(match));
            }
        }
        return new FootbarSessionSetup(matchDTOList.isEmpty() ? null : matchDTOList.get(0),
                seasonId == null ? seasonService.getAllSeason() :
                        seasonService.getSeason(seasonId), matchDTOList, footbarAccountSessions);
    }

    public double getTotalDistanceForPlayerAndSeason(long playerId, long seasonId) {
        return footbarSessionGetter.getTotalDistanceForPlayerAndSeason(playerId, seasonId);
    }
}