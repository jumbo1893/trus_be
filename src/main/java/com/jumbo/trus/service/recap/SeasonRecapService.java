package com.jumbo.trus.service.recap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.SeasonRecapEntity;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.*;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static com.jumbo.trus.service.recap.SeasonRecapBuilder.*;

@Service @RequiredArgsConstructor @Slf4j
public class SeasonRecapService {
    private final SeasonRecapRepository repository;
    private final SeasonRepository seasons;
    private final UserRepository users;
    private final UserTeamRoleRepository roles;
    private final SeasonRecapData data;
    private final SeasonRecapBuilder builder;
    private final ObjectMapper mapper;
    private final AfterCommitExecutor afterCommit;
    private final DeviceTokenRepository tokens;
    private final PushService push;
    private Clock clock = Clock.system(ZoneId.of("Europe/Prague"));
    public record Summary(long id, String seasonName, String from, String to, boolean opened) {}
    public record TestPublication(long recapId, String seasonName, boolean opened, boolean pushScheduled) {}

    /** Called only by the dev/test controller; no other user's summaries are touched. */
    @Transactional
    public TestPublication testPublishLatest(long user, long team) {
        var latest=repository.findByUserIdAndSeasonAppTeamIdOrderBySeasonToDateDescIdDesc(user,team)
                .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pro tento účet a tým zatím neexistuje sezonní souhrn."));
        var entity=owned(latest.getId(),user);
        var recap=read(entity);
        entity.setOpenedAt(null);
        repository.saveAndFlush(entity);
        long recapId=entity.getId();
        afterCommit.execute("season-recap-test-push "+recapId,
                ()->notifyUser(user,team,recapId,recap.seasonName()));
        return new TestPublication(recapId,recap.seasonName(),false,true);
    }

    /** Nine the next day; hourly catch-up if the server was down. Historic imports do not flood users with push. */
    @Scheduled(cron="0 0 9-23 * * *", zone="Europe/Prague")
    @Transactional
    public void publishDue() {
        if(!repository.tryPublicationLock())return;
        LocalDate today=LocalDate.now(clock);
        Map<Long,LocalDate> previousEnds=new HashMap<>();
        for(var season:data.seasons()) {
            long team=id(season,"app_team_id"), seasonId=id(season,"id");
            LocalDate end=date(season.get("last_match"));
            LocalDate seasonBoundary=date(season.get("to_date"));
            LocalDate previous=previousEnds.get(team);
            LocalDate from=previous==null?date(season.get("from_date")):previous.plusDays(1);
            // Use the known season boundary as well: a partially imported schedule must not publish after its first match.
            if(!end.isBefore(today)||!seasonBoundary.isBefore(today))continue;
            if(previous==null||end.isAfter(previous))previousEnds.put(team,end);
            if(from.isAfter(end))continue;
            SeasonRecapData.Facts facts=null;
            Set<Long> seen=new HashSet<>();
            for(var member:data.members(team)) {
                long user=id(member,"user_id");
                if(!seen.add(user)||repository.existsBySeasonIdAndUserId(seasonId,user))continue;
                if(facts==null)facts=data.load(team,seasonId,from,end);
                Long player=member.get("player_id") instanceof Number p?p.longValue():null;
                var recap=builder.build(String.valueOf(season.get("name")),from,end,player,user,facts);
                var entity=new SeasonRecapEntity(); entity.setSeason(seasons.getReferenceById(seasonId));entity.setUser(users.getReferenceById(user));
                try {entity.setSnapshot(mapper.writeValueAsString(recap));}catch(Exception e){throw new IllegalStateException("Cannot serialize season recap",e);}
                entity.setPublishedAt(clock.instant()); repository.saveAndFlush(entity);
                LocalDate publicationDay=(end.isAfter(seasonBoundary)?end:seasonBoundary).plusDays(1);
                if(publicationDay.equals(today)) {
                    long recapId=entity.getId();String name=recap.seasonName();
                    afterCommit.execute("season-recap-push "+recapId,()->notifyUser(user,team,recapId,name));
                }
            }
        }
    }
    private void notifyUser(long user,long team,long id,String name) {
        Set<String> seen=new HashSet<>();
        for(var token:tokens.findByUser_IdIn(List.of(user))) {
            if(!"ACTIVE".equals(token.getStatus())||token.getToken()==null||!seen.add(token.getToken()))continue;
            try {push.sendPush(token,"Tvoje sezona "+name,"Souhrn je připravený. Pojď se podívat, co všechno se stalo!",NotificationType.GLOBAL,
                    Map.of("type","GLOBAL","screenId","season-recap","recapId",Long.toString(id),"appTeamId",Long.toString(team)));}
            catch(Exception e){log.warn("Season recap push failed recapId={}, tokenId={}",id,token.getId(),e);}
        }
    }
    @Transactional(readOnly=true)
    public List<Summary> list(long user,long team) {
        return repository.findByUserIdAndSeasonAppTeamIdOrderBySeasonToDateDescIdDesc(user,team).stream().map(e->{var r=read(e);return new Summary(e.getId(),r.seasonName(),r.from(),r.to(),e.getOpenedAt()!=null);}).toList();
    }
    @Transactional(readOnly=true)
    public SeasonRecap detail(long id,long user) {
        var entity=owned(id,user);
        long team=entity.getSeason().getAppTeam().getId();
        var recap=read(entity);
        // Do not expose archived named step rankings after someone revokes team sharing.
        var consent=data.consentingUsers(team);
        var pages=recap.pages().stream().map(p->{
            if(!p.kind().equals("steps"))return p;
            var metrics=consent.contains(user)?p.metrics():p.metrics().stream().filter(m->m.label().startsWith("Celkem")).toList();
            return new SeasonRecap.Page(p.kind(),p.title(),p.text(),metrics,p.boards().stream().map(b->new SeasonRecap.Board(b.title(),b.unit(),b.rows().stream().filter(r->consent.contains(r.subjectId())).toList())).toList());
        }).toList();
        return new SeasonRecap(recap.seasonName(),recap.from(),recap.to(),pages);
    }
    @Transactional
    public void opened(long id,long user) {var entity=owned(id,user);if(entity.getOpenedAt()==null)entity.setOpenedAt(clock.instant());}
    /** Explicit owner-only refresh: preserve the archived period, never notify other members. */
    @Transactional
    public Summary regenerate(long id,long user,boolean resetOpened) {
        var entity=owned(id,user);
        var original=read(entity);
        long team=entity.getSeason().getAppTeam().getId();
        var role=roles.findByUserIdAndAppTeamId(user,team).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Long player=role.getPlayer()==null?null:role.getPlayer().getId();
        var from=LocalDate.parse(original.from());var to=LocalDate.parse(original.to());
        var recap=builder.build(entity.getSeason().getName(),from,to,player,user,
                data.load(team,entity.getSeason().getId(),from,to));
        try {entity.setSnapshot(mapper.writeValueAsString(recap));}
        catch(Exception e){throw new IllegalStateException("Cannot serialize season recap",e);}
        if(resetOpened)entity.setOpenedAt(null);
        repository.saveAndFlush(entity);
        return new Summary(id,recap.seasonName(),recap.from(),recap.to(),entity.getOpenedAt()!=null);
    }
    // Push links can open another team without silently switching the user's selected team.
    // Both ownership and current membership of the snapshot's team are mandatory.
    private SeasonRecapEntity owned(long id,long user){
        var entity=repository.findByIdAndUserId(id,user).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var role=roles.findByUserIdAndAppTeamId(user,entity.getSeason().getAppTeam().getId());
        if(role.isEmpty()||!Set.of("READER","EDITOR","ADMIN").contains(role.get().getRole().toUpperCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return entity;
    }
    private SeasonRecap read(SeasonRecapEntity entity){try{return mapper.readValue(entity.getSnapshot(),SeasonRecap.class);}catch(Exception e){throw new IllegalStateException("Cannot read season recap",e);}}
}
