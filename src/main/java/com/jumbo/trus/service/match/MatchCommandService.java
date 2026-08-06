package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.specification.MatchSpecification;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.weather.WeatherService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.AUTOMATIC_SEASON_ID;

@Service
@RequiredArgsConstructor
public class MatchCommandService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;
    private final MatchMapper matchMapper;
    private final FootballMatchMapper footballMatchMapper;
    private final SeasonService seasonService;
    private final FootballMatchService footballMatchService;
    private final NotificationService notificationService;
    private final MatchResultFineService matchResultFineService;
    private final WeatherService weatherService;
    private final OutboxEventService outboxEventService;

    /**
     * Spáruje pouze zápasy z předaného app teamu. Původní implementace
     * načetla zápasy všech týmů a následně jim přepsala appTeam.
     */
    @Transactional
    public void pairAllFootballMatches(AppTeamEntity appTeam) {
        MatchFilter matchFilter = new MatchFilter(appTeam);
        MatchSpecification specification = new MatchSpecification(matchFilter);

        List<MatchEntity> matches = matchRepository.findAll(
                specification,
                PageRequest.of(0, matchFilter.getLimit())
        ).getContent();

        for (MatchEntity match : matches) {
            FootballMatchDTO footballMatch = footballMatchService.getFootballMatchByDate(
                    match.getDate(),
                    appTeam
            );
            match.setFootballMatch(footballMatchMapper.toEntity(footballMatch));
        }

        matchRepository.saveAll(matches);
    }

    @Transactional
    public MatchDTO addMatch(MatchDTO matchDTO, AppTeamEntity appTeam) {
        MatchEntity match = matchMapper.toEntity(matchDTO);
        applyEditableFields(match, matchDTO, appTeam);

        weatherService.createWeatherForMatch(match)
                .ifPresent(match::setWeather);

        MatchEntity savedMatch = matchRepository.save(match);
        matchResultFineService.rewriteAutomaticFines(savedMatch, appTeam);
        publishMatchCreated(savedMatch, matchDTO);

        return matchMapper.toDTO(savedMatch);
    }

    @Transactional
    public MatchDTO editMatch(
            Long matchId,
            MatchDTO matchDTO,
            AppTeamEntity appTeam
    ) throws NotFoundException {
        MatchEntity match = findMatchForTeamOrThrow(matchId, appTeam.getId());
        Set<Long> originalPlayerIds = getPlayerIds(match);

        matchMapper.updateEntity(matchDTO, match);
        applyEditableFields(match, matchDTO, appTeam);

        PlayerChanges playerChanges = PlayerChanges.between(
                originalPlayerIds,
                getPlayerIds(match)
        );

        MatchEntity savedMatch = matchRepository.save(match);
        matchResultFineService.rewriteAutomaticFines(savedMatch, appTeam);
        publishMatchUpdated(savedMatch, matchDTO, playerChanges);

        return matchMapper.toDTO(savedMatch);
    }

    @Transactional
    public void deleteMatch(Long matchId, AppTeamEntity appTeam) throws NotFoundException {
        MatchEntity match = findMatchForTeamOrThrow(matchId, appTeam.getId());
        String description = createMatchDescription(matchMapper.toDTO(match));
        Long seasonId = match.getSeason() == null ? null : match.getSeason().getId();
        Set<Long> removedPlayerIds = getPlayerIds(match);

        matchRepository.delete(match);

        notificationService.addNotification("Smazán zápas", description);
        outboxEventService.createEvent(
                OutboxEventType.MATCH_DELETED,
                OutboxAggregateType.MATCH,
                matchId,
                OutboxEventPayloadFactory.matchDeleted(
                        matchId,
                        seasonId,
                        removedPlayerIds
                )
        );
    }

    private void applyEditableFields(
            MatchEntity match,
            MatchDTO matchDTO,
            AppTeamEntity appTeam
    ) {
        match.setAppTeam(appTeam);
        match.setPlayerList(loadPlayers(matchDTO.getPlayerIdList(), appTeam.getId()));
        match.setSeason(resolveSeason(matchDTO, appTeam));
        match.setFootballMatch(resolveFootballMatch(matchDTO));
    }

    private List<PlayerEntity> loadPlayers(
            Collection<Long> requestedPlayerIds,
            Long appTeamId
    ) {
        if (requestedPlayerIds == null || requestedPlayerIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> uniquePlayerIds = new ArrayList<>(new LinkedHashSet<>(requestedPlayerIds));
        Map<Long, PlayerEntity> playersById = new HashMap<>();
        playerRepository.findAllByIdsAndAppTeam(uniquePlayerIds, appTeamId)
                .forEach(player -> playersById.put(player.getId(), player));

        Set<Long> missingPlayerIds = uniquePlayerIds.stream()
                .filter(playerId -> !playersById.containsKey(playerId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!missingPlayerIds.isEmpty()) {
            throw new EntityNotFoundException(
                    "Hráči s id " + missingPlayerIds
                            + " nebyli nalezeni v týmu " + appTeamId
            );
        }

        return uniquePlayerIds.stream()
                .map(playersById::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private SeasonEntity resolveSeason(MatchDTO matchDTO, AppTeamEntity appTeam) {
        Long seasonId = matchDTO.getSeasonId();

        if (Objects.equals(seasonId, AUTOMATIC_SEASON_ID)) {
            seasonId = seasonService
                    .getSeasonByDateOrOther(matchDTO.getDate(), appTeam)
                    .getId();
        }

        Long resolvedSeasonId = seasonId;
        return seasonRepository.findByIdAndAppTeamId(resolvedSeasonId, appTeam.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sezona s id " + resolvedSeasonId
                                + " nebyla nalezena v týmu " + appTeam.getId()
                ));
    }

    private FootballMatchEntity resolveFootballMatch(MatchDTO matchDTO) {
        if (matchDTO.getFootballMatch() == null) {
            return null;
        }

        Long footballMatchId = matchDTO.getFootballMatch().getId();
        FootballMatchDTO footballMatch = footballMatchService.getFootballMatchById(footballMatchId);
        return footballMatchMapper.toEntity(footballMatch);
    }

    private MatchEntity findMatchForTeamOrThrow(
            Long matchId,
            Long appTeamId
    ) throws NotFoundException {
        return matchRepository.findByIdAndAppTeamId(matchId, appTeamId)
                .orElseThrow(() -> new NotFoundException(
                        "Zápas s id " + matchId + " nebyl nalezen v týmu " + appTeamId
                ));
    }

    private void publishMatchCreated(MatchEntity match, MatchDTO source) {
        notificationService.addNotification(
                "Přidán nový zápas",
                createMatchDescription(source)
        );

        outboxEventService.createEvent(
                OutboxEventType.MATCH_CREATED,
                OutboxAggregateType.MATCH,
                match.getId(),
                OutboxEventPayloadFactory.matchAdded(
                        match.getId(),
                        match.getSeason().getId(),
                        getPlayerIds(match)
                )
        );
    }

    private void publishMatchUpdated(
            MatchEntity match,
            MatchDTO source,
            PlayerChanges playerChanges
    ) {
        notificationService.addNotification(
                "Upraven zápas",
                createMatchDescription(source)
        );

        outboxEventService.createEvent(
                OutboxEventType.MATCH_UPDATED,
                OutboxAggregateType.MATCH,
                match.getId(),
                OutboxEventPayloadFactory.matchChanged(
                        match.getId(),
                        match.getSeason().getId(),
                        playerChanges.addedPlayerIds(),
                        playerChanges.removedPlayerIds()
                )
        );
    }

    private String createMatchDescription(MatchDTO matchDTO) {
        return new MatchHelper(matchDTO).getMatchWithOpponentNameAndDate();
    }

    private Set<Long> getPlayerIds(MatchEntity match) {
        if (match.getPlayerList() == null) {
            return Set.of();
        }

        return match.getPlayerList().stream()
                .map(PlayerEntity::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record PlayerChanges(
            Set<Long> addedPlayerIds,
            Set<Long> removedPlayerIds
    ) {

        private static PlayerChanges between(
                Set<Long> originalPlayerIds,
                Set<Long> currentPlayerIds
        ) {
            Set<Long> addedPlayerIds = new HashSet<>(currentPlayerIds);
            addedPlayerIds.removeAll(originalPlayerIds);

            Set<Long> removedPlayerIds = new HashSet<>(originalPlayerIds);
            removedPlayerIds.removeAll(currentPlayerIds);

            return new PlayerChanges(
                    Set.copyOf(addedPlayerIds),
                    Set.copyOf(removedPlayerIds)
            );
        }
    }
}
