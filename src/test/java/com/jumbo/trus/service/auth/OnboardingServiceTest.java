package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.repository.auth.*;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.repository.football.FootballPlayerRepository;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OnboardingServiceTest {
    final UserService userService = mock(UserService.class);
    final UserRepository users = mock(UserRepository.class);
    final AppTeamService teams = mock(AppTeamService.class);
    final UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
    final PlayerService players = mock(PlayerService.class);
    final FootballPlayerRepository footballPlayers = mock(FootballPlayerRepository.class);
    final OnboardingService service = new OnboardingService(userService, users, teams, roles, players, footballPlayers);
    final UserEntity user = new UserEntity();
    final AppTeamEntity team = new AppTeamEntity();
    final UserTeamRole role = new UserTeamRole();

    @BeforeEach void setup() {
        user.setId(1L); user.setName("Matěj Novák"); team.setId(2L);
        role.setUser(user); role.setAppTeam(team);
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(userService.findById(1L)).thenReturn(user);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(teams.getCurrentAppTeamOrThrow()).thenReturn(team);
        when(roles.findByUserIdAndAppTeamId(1L, 2L)).thenReturn(Optional.of(role));
    }

    @Test void existingAccountsDoNotGetAutomaticOnboarding() {
        assertFalse(service.state().available()); assertFalse(service.state().autoShow());
        var manuallyOpened = service.advance(null);
        assertTrue(manuallyOpened.available()); assertFalse(manuallyOpened.autoShow());
    }

    @Test void newAccountsOpenOnceAndSkippingKeepsIncompleteSteps() {
        user.setOnboardingCompleted(0);
        assertTrue(service.state().autoShow());
        service.advance(null);
        assertFalse(service.state().autoShow()); assertTrue(service.state().completed().isEmpty());
        service.advance(OnboardingService.Step.INTRO);
        service.advance(OnboardingService.Step.INTRO);
        service.advance(OnboardingService.Step.STEPS);
        assertEquals(Set.of(OnboardingService.Step.INTRO, OnboardingService.Step.STEPS), service.state().completed());
    }

    @Test void similarityHandlesDiacriticsWordOrderAndTypos() {
        assertEquals(100, OnboardingService.similarity("Matěj Novák", "matej novak"));
        assertEquals(80, OnboardingService.similarity("Matěj Novák", "Novák Matěj"));
        assertTrue(OnboardingService.similarity("Matej", "Mate") >= 65);
        assertEquals(0, OnboardingService.similarity(null, "Matěj"));
    }

    @Test void occupiedProfilesAreLastAndRealNameAlsoMatches() {
        PlayerDTO free = dto(3, "Přezdívka");
        FootballPlayerDTO football = new FootballPlayerDTO(); football.setName("Matěj Novák"); free.setFootballPlayer(football);
        PlayerDTO taken = dto(4, "Matěj Novák");
        UserEntity other = new UserEntity(); other.setId(5L);
        UserTeamRole assignment = new UserTeamRole(); assignment.setUser(other);
        PlayerEntity occupied = new PlayerEntity(); occupied.setId(4); assignment.setPlayer(occupied);
        when(roles.findAllByAppTeamId(2L)).thenReturn(List.of(role, assignment));
        when(players.getAll(2L)).thenReturn(List.of(taken, free));
        var result = service.profiles();
        assertEquals(3, result.candidates().get(0).player().getId());
        assertEquals(100, result.candidates().get(0).score());
        assertTrue(result.candidates().get(1).occupied());
    }

    @Test void pairingUsesSharedGuardAndDoesNotCompleteOnConflict() {
        doThrow(new FieldValidationException()).when(teams).pairPlayerToRole(role, 1L, 4L, team);
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(4L, null, null, false)));
        assertNull(user.getOnboardingCompleted());
        verify(players, never()).addPlayer(any(), any());
    }

    @Test void createsOwnFanAndPairsWithoutGrantingExtraRights() {
        role.setRole("READER");
        PlayerDTO created = dto(8, "Matěj");
        when(players.addPlayer(any(), eq(team))).thenAnswer(inv -> {
            PlayerDTO request = inv.getArgument(0);
            assertEquals("Matěj", request.getName()); assertTrue(request.isFan()); assertTrue(request.isActive());
            assertNotNull(request.getBirthday()); return created;
        });
        when(players.getPlayer(8)).thenReturn(created);
        assertSame(created, service.pair(new OnboardingService.PairRequest(null, " Matěj ", LocalDate.of(1995, 1, 2), true)));
        verify(teams).pairPlayerToRole(role, 1L, 8L, team);
        assertEquals("READER", role.getRole());
        assertTrue(service.state().completed().contains(OnboardingService.Step.PROFILE));
    }

    @Test void retryAfterCreationReturnsExistingProfileWithoutDuplicates() {
        PlayerEntity existing = new PlayerEntity(); existing.setId(8); role.setPlayer(existing);
        when(players.getPlayer(8)).thenReturn(dto(8, "Matěj"));
        assertEquals(8, service.pair(new OnboardingService.PairRequest(null, "Matěj", LocalDate.of(1995, 1, 2), false)).getId());
        verify(players, never()).addPlayer(any(), any());
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(9L, null, null, false)));
    }

    @Test void invalidBirthdaysAndNamesCannotCreatePlayers() {
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(null, " ", LocalDate.of(1995, 1, 2), false)));
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(null, "Matěj", null, false)));
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(null, "Matěj", LocalDate.now().plusDays(2), false)));
        verify(players, never()).addPlayer(any(), any());
    }

    PlayerDTO dto(long id, String name) { PlayerDTO p = new PlayerDTO(); p.setId(id); p.setName(name); return p; }

    private FootballPlayerEntity footballSetup() {
        TeamEntity footballTeam = new TeamEntity(); footballTeam.setId(20L); team.setTeam(footballTeam);
        FootballPlayerEntity football = new FootballPlayerEntity(); football.setId(30L); football.setName("Fotbalista");
        when(footballPlayers.findAllByTeamId(20L)).thenReturn(List.of(football));
        when(footballPlayers.findAllByTeamIdWithInactive(20L)).thenReturn(List.of(football));
        when(footballPlayers.findByIdForUpdate(30L)).thenReturn(Optional.of(football));
        PlayerEntity player = new PlayerEntity(); player.setId(8L); role.setPlayer(player);
        when(players.getPlayer(8L)).thenReturn(dto(8L, "Přezdívka"));
        return football;
    }

    @Test void linksOwnProfileToTeamFootballPlayerAndRunsNormalEditLifecycle() {
        footballSetup();
        service.pair(new OnboardingService.PairRequest(8L, null, null, false, 30L));
        verify(players).editPlayer(eq(8L), argThat(p -> p.getFootballPlayer().getId().equals(30L)));
    }

    @Test void refusesFootballPlayerFromAnotherTeam() {
        footballSetup();
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(8L, null, null, false, 99L)));
        verify(players, never()).editPlayer(anyLong(), any());
    }

    @Test void occupiedFootballPlayerCannotBeTakenOver() {
        var football = footballSetup(); PlayerEntity other = new PlayerEntity(); other.setId(9L); football.setPlayer(other);
        assertThrows(FieldValidationException.class, () -> service.pair(new OnboardingService.PairRequest(8L, null, null, false, 30L)));
        verify(players, never()).editPlayer(anyLong(), any());
        var profiles = service.profiles();
        assertEquals(1, profiles.footballPlayers().size());
        assertEquals(9L, profiles.footballPlayers().get(0).linkedPlayerId());
    }
}
