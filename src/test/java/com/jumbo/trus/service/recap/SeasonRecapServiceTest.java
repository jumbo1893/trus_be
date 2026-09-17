package com.jumbo.trus.service.recap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.repository.*;
import com.jumbo.trus.repository.auth.*;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static com.jumbo.trus.service.recap.SeasonRecapBuilderTest.row;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeasonRecapServiceTest {
    SeasonRecapRepository repo=mock(SeasonRecapRepository.class);
    SeasonRepository seasons=mock(SeasonRepository.class);
    UserRepository users=mock(UserRepository.class);
    UserTeamRoleRepository roles=mock(UserTeamRoleRepository.class);
    SeasonRecapData data=mock(SeasonRecapData.class);
    SeasonRecapBuilder builder=mock(SeasonRecapBuilder.class);
    ObjectMapper mapper=new ObjectMapper();
    AfterCommitExecutor after=mock(AfterCommitExecutor.class);
    SeasonRecapService service=new SeasonRecapService(repo,seasons,users,roles,data,builder,mapper,after,mock(DeviceTokenRepository.class),mock(PushService.class));
    SeasonRecap snapshot=new SeasonRecap("Podzim 2026","2026-06-02","2026-12-01",List.of());
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(Instant.parse("2026-12-02T08:00:00Z"),ZoneId.of("Europe/Prague")));
        when(repo.tryPublicationLock()).thenReturn(true);
        when(repo.saveAndFlush(any())).thenAnswer(i->{SeasonRecapEntity e=i.getArgument(0);e.setId(123L);return e;});
        when(builder.build(anyString(),any(),any(),any(),anyLong(),any())).thenReturn(snapshot);
    }
    Map<String,Object> season(long id,String start,String end) {
        return row("id",id,"app_team_id",5L,"name","Sezona "+id,"from_date",java.sql.Date.valueOf(start),"to_date",java.sql.Date.valueOf(end),"last_match",java.sql.Date.valueOf(end));
    }
    @Test void usesDayAfterPreviousLastMatchAndOnlyPushesNewPublication() {
        when(data.seasons()).thenReturn(List.of(season(1,"2026-03-01","2026-06-01"),season(2,"2026-09-01","2026-12-01"),season(3,"2027-03-01","2027-06-01")));
        when(data.members(5)).thenReturn(List.of(row("user_id",7L,"player_id",8L),row("user_id",7L,"player_id",8L)));
        service.publishDue();
        verify(data).load(5,2,LocalDate.parse("2026-06-02"),LocalDate.parse("2026-12-01"));
        verify(repo,times(2)).saveAndFlush(any());
        verify(after,times(1)).execute(anyString(),any());
        verify(data,never()).load(eq(5L),eq(3L),any(),any());
    }
    @Test void existingSnapshotIsNeverReplacedOrNotifiedAgain() {
        when(data.seasons()).thenReturn(List.of(season(2,"2026-09-01","2026-12-01")));
        when(data.members(5)).thenReturn(List.of(row("user_id",7L,"player_id",8L)));
        when(repo.existsBySeasonIdAndUserId(2L,7L)).thenReturn(true);
        service.publishDue();
        verify(repo,never()).saveAndFlush(any());verifyNoInteractions(after);
    }
    @Test void futureSeasonBoundaryPreventsPrematurePublication() {
        var s=season(2,"2026-09-01","2026-12-10");s.put("last_match",java.sql.Date.valueOf("2026-12-01"));
        when(data.seasons()).thenReturn(List.of(s));service.publishDue();
        verify(data,never()).members(anyLong());
    }
    @Test void concurrentReplicaDoesNoWork() {
        when(repo.tryPublicationLock()).thenReturn(false);service.publishDue();verifyNoInteractions(data);
    }
    SeasonRecapEntity owned() throws Exception {
        var team=new AppTeamEntity();team.setId(5L);
        var season=new SeasonEntity();season.setAppTeam(team);season.setId(2L);season.setName("Podzim 2026");
        var entity=new SeasonRecapEntity();entity.setId(123L);entity.setSeason(season);entity.setSnapshot(mapper.writeValueAsString(snapshot));
        when(repo.findByIdAndUserId(123L,7L)).thenReturn(Optional.of(entity));
        var role=new UserTeamRole();role.setRole("READER");
        when(roles.findByUserIdAndAppTeamId(7L,5L)).thenReturn(Optional.of(role));
        return entity;
    }
    @Test void openingIsIdempotentAndRequiresOwnershipAndMembership() throws Exception {
        var e=owned();service.opened(123,7);var first=e.getOpenedAt();service.opened(123,7);
        assertThat(e.getOpenedAt()).isEqualTo(first).isNotNull();
        assertThatThrownBy(()->service.opened(123,8)).isInstanceOf(ResponseStatusException.class);
        when(roles.findByUserIdAndAppTeamId(7L,5L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.detail(123,7)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void testPublicationResetsLatestOwnedRecapAndSchedulesPushWithoutRegeneration() throws Exception {
        var entity=owned();entity.setOpenedAt(Instant.now());
        var original=entity.getSnapshot();var published=entity.getPublishedAt();
        when(repo.findByUserIdAndSeasonAppTeamIdOrderBySeasonToDateDescIdDesc(7L,5L)).thenReturn(List.of(entity));
        var result=service.testPublishLatest(7,5);
        assertThat(result.recapId()).isEqualTo(123);
        assertThat(result.opened()).isFalse();assertThat(result.pushScheduled()).isTrue();
        assertThat(entity.getOpenedAt()).isNull();
        assertThat(entity.getSnapshot()).isEqualTo(original);
        assertThat(entity.getPublishedAt()).isEqualTo(published);
        verify(repo).saveAndFlush(entity);
        verify(after).execute(eq("season-recap-test-push 123"),any(Runnable.class));
        verifyNoInteractions(data);
    }
    @Test void missingTestRecapDoesNotSendPush() {
        assertThatThrownBy(()->service.testPublishLatest(7,5)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(after);
    }
    @Test void regeneratesOnlyOwnedSnapshotWithOriginalPeriodAndNoPush() throws Exception {
        var entity=owned();
        entity.setOpenedAt(Instant.parse("2026-12-01T12:00:00Z"));
        entity.setPublishedAt(Instant.parse("2026-12-01T08:00:00Z"));
        var firstPublished=entity.getPublishedAt();
        var result=service.regenerate(123,7,false);
        assertThat(result.opened()).isTrue();
        assertThat(entity.getPublishedAt()).isEqualTo(firstPublished);
        verify(data).load(5,2,LocalDate.parse("2026-06-02"),LocalDate.parse("2026-12-01"));
        verify(repo).saveAndFlush(entity);
        verifyNoInteractions(after);
        assertThat(service.regenerate(123,7,true).opened()).isFalse();
        assertThat(entity.getOpenedAt()).isNull();
        assertThatThrownBy(()->service.regenerate(123,8,true)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void revokedStepSharingHidesNamedRowsButKeepsGeneralTotals() throws Exception {
        snapshot=new SeasonRecap("Test","2026-06-02","2026-12-01",List.of(new SeasonRecap.Page("steps","Kroky","",List.of(
            new SeasonRecap.Metric("Tvoje kroky","100"),new SeasonRecap.Metric("Celkem sdílených kroků týmu","300")),
            List.of(new SeasonRecap.Board("Pořadí","kroků",List.of(new SeasonRecap.Standing(7,"Jan",1,100,true),new SeasonRecap.Standing(8,"Petr",2,50,false)))))));
        owned();when(data.consentingUsers(5)).thenReturn(Set.of(8L));
        var page=service.detail(123,7).pages().get(0);
        assertThat(page.metrics()).hasSize(1);
        assertThat(page.boards().get(0).rows()).extracting(SeasonRecap.Standing::subjectId).containsExactly(8L);
    }
}
