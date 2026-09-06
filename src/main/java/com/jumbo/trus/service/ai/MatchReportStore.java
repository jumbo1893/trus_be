package com.jumbo.trus.service.ai;

import com.jumbo.trus.entity.ai.MatchReportEntity;
import com.jumbo.trus.entity.ai.MatchReportGeneration;
import com.jumbo.trus.entity.ai.MatchReportStyle;
import com.jumbo.trus.entity.ai.AiAccessTier;
import com.jumbo.trus.dto.ai.MatchReportDTO;
import com.jumbo.trus.dto.ai.MatchReportStateDTO;
import com.jumbo.trus.repository.ai.MatchReportGenerationRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.config.AiOpenAiProperties;
import com.jumbo.trus.service.exceptions.AiUnavailableException;
import com.jumbo.trus.service.exceptions.AuthException;
import java.time.Instant;
import java.util.UUID;
import com.jumbo.trus.repository.ai.MatchReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchReportStore {
    private final MatchReportRepository repository;
    private final AiQuotaService quota;
    private final MatchReportGenerationRepository generations;
    private final AppTeamRepository teams;
    private final AiOpenAiProperties properties;

    public MatchReportStateDTO state(Long teamId, Long matchId, Long userId) {
        var report = repository.findFirstByAppTeamIdAndFootballMatchIdAndStyleOrderByIdDesc(
                teamId, matchId, MatchReportStyle.FUN).map(MatchReportDTO::from).orElse(null);
        boolean generating = generations.findById(key(teamId, matchId))
                .map(g -> g.getExpiresAt().isAfter(Instant.now())).orElse(false);
        boolean canGenerate = !repository.existsByAppTeamIdAndFootballMatchId(teamId, matchId) || isUltra(userId);
        return new MatchReportStateDTO(report, canGenerate && !generating, generating);
    }

    @Transactional
    public String begin(Long teamId, Long matchId, Long userId) {
        lockTeam(teamId);
        var generation = generations.findById(key(teamId, matchId)).orElse(null);
        if (generation != null && generation.getExpiresAt().isAfter(Instant.now())) {
            throw new AiUnavailableException("Report právě generuje jiný člen týmu. Za chvíli jej znovu načtěte.");
        }
        if (repository.existsByAppTeamIdAndFootballMatchId(teamId, matchId) && !isUltra(userId)) {
            throw new AuthException("Nový report může vygenerovat pouze člen s členstvím ULTRA.", AuthException.INSUFFICIENT_RIGHTS);
        }
        generation = new MatchReportGeneration();
        generation.setId(key(teamId, matchId));
        generation.setToken(UUID.randomUUID().toString());
        // Expired claims can be recovered after a crashed process. Old workers cannot save over a new claim.
        generation.setExpiresAt(Instant.now().plusSeconds(Math.max(10, properties.getTimeoutSeconds()) + 180L));
        generations.save(generation);
        return generation.getToken();
    }

    @Transactional
    public MatchReportEntity save(MatchReportEntity report, Long questionId, OpenAiAnswer answer, String token) {
        lockTeam(report.getAppTeamId());
        var generation = generations.findById(key(report.getAppTeamId(), report.getFootballMatchId())).orElse(null);
        if (generation == null || !generation.getToken().equals(token)) {
            throw new AiUnavailableException("Generování již není aktuální. Načtěte uložený report.");
        }
        quota.complete(questionId, answer);
        MatchReportEntity saved = repository.save(report);
        generations.delete(generation);
        return saved;
    }

    @Transactional
    public void release(Long teamId, Long matchId, String token) {
        lockTeam(teamId);
        generations.findById(key(teamId, matchId)).filter(g -> g.getToken().equals(token))
                .ifPresent(generations::delete);
    }

    private boolean isUltra(Long userId) {
        return quota.getUsage(userId).getTier() == AiAccessTier.ULTRA;
    }

    private void lockTeam(Long teamId) {
        teams.findForReportUpdate(teamId).orElseThrow(() ->
                new AuthException("Tým není dostupný.", AuthException.INSUFFICIENT_RIGHTS));
    }

    private String key(Long teamId, Long matchId) { return teamId + ":" + matchId; }
}
