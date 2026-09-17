package com.jumbo.trus.service.recap;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class SeasonRecapBuilderTest {
    final SeasonRecapBuilder builder = new SeasonRecapBuilder();
    static Map<String,Object> row(Object... pairs) {
        var map = new HashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]);
        return map;
    }
    SeasonRecapData.Facts facts(boolean fan) {
        return new SeasonRecapData.Facts(
            List.of(row("id",1L,"name","Jan","fan",fan), row("id",2L,"name","Petr","fan",false)),
            List.of(row("id",10L,"name","Trus – Hosté","date",java.sql.Date.valueOf("2026-12-01")),row("id",11L,"name","Trus – Jiní","date",java.sql.Date.valueOf("2026-11-01"))),
            List.of(row("player_id",1L,"match_id",10L,"beers",4,"shots",2),row("player_id",2L,"match_id",10L,"beers",6,"shots",3)),
            List.of(row("player_id",1L,"match_id",10L,"name","Kopačky","amount",200,"count",2,"unit_amount",100)),
            List.of(row("player_id",1L,"match_id",10L,"goals",2,"assists",1)),
            List.of(row("player_id",1L,"achievement_id",1L,"name","Střelky","holders",1,"eligible",2)),
            List.of(row("player_id",100L,"name","Jan","step_date","2026-06-02","steps",1000),row("player_id",100L,"name","Jan","step_date","2026-11-01","steps",3000)),
            List.of(row("player_id",1L,"match_id",10L,"shots",3,"passes",20,"km",5.5,"speed",90)),
            List.of(row("player_id",1L,"match_id",10L),row("player_id",2L,"match_id",10L)));
    }
    SeasonRecap recap(Long player, boolean fan) {
        return builder.build("Podzim 2026",LocalDate.parse("2026-06-02"),LocalDate.parse("2026-12-01"),player,100L,facts(fan));
    }
    SeasonRecap.Page page(SeasonRecap r,String kind){return r.pages().stream().filter(p->p.kind().equals(kind)).findFirst().orElseThrow();}
    @Test void buildsNinePagesWithPersonalAndTeamTotals() {
        var r=recap(1L,false);
        assertThat(r.pages()).hasSize(9);
        assertThat(r.from()).isEqualTo("2026-06-02");
        assertThat(page(r,"drinks").metrics()).contains(new SeasonRecap.Metric("Ty · Piva","4"),new SeasonRecap.Metric("Tým · Piva","10"));
        assertThat(page(r,"fines").metrics()).contains(new SeasonRecap.Metric("Ty · Pokuty (Kč)","200"));
        assertThat(page(r,"goals").metrics()).anyMatch(m->m.label().equals("Nejvíc kanadských bodů")&&m.value().endsWith(" · 3"));
        assertThat(page(r,"footbar").metrics()).contains(new SeasonRecap.Metric("Tvoje nejrychlejší střela","90 km/h"));
        assertThat(page(r,"attendance").metrics()).contains(new SeasonRecap.Metric("Tvoje účasti","1"));
        assertThat(page(r,"attendance").metrics()).anyMatch(m->m.label().equals("Nejnižší účast")&&m.value().endsWith("0 lidí"));
    }
    @Test void fanOrUnpairedKeepsGeneralDataWithoutFabricatedPersonalFootballStats() {
        var fan=recap(1L,true);
        assertThat(page(fan,"goals").metrics()).noneMatch(m->m.label().startsWith("Ty"));
        var unpaired=recap(null,false);
        assertThat(unpaired.pages()).hasSize(9);
        assertThat(page(unpaired,"drinks").metrics()).hasSize(2);
        assertThat(page(unpaired,"steps").metrics()).hasSize(1);
        assertThat(page(unpaired,"footbar").metrics()).noneMatch(m->m.label().startsWith("Tvoje"));
    }
    @Test void leaderboardShowsWinnerSelfAndBothNeighboursWithCompetitionRanks() {
        var people=new ArrayList<Map<String,Object>>();var facts=new ArrayList<Map<String,Object>>();
        for(long i=1;i<=7;i++){people.add(row("id",i,"name","Hráč "+i));facts.add(row("player_id",i,"score",10-i));}
        var board=SeasonRecapBuilder.board("Pořadí","bodů",people,facts,"score",5L);
        assertThat(board.rows()).extracting(SeasonRecap.Standing::subjectId).containsExactly(1L,4L,5L,6L);
        assertThat(board.rows()).filteredOn(SeasonRecap.Standing::mine).extracting(SeasonRecap.Standing::rank).containsExactly(5);
        facts.get(1).put("score",9);
        assertThat(SeasonRecapBuilder.board("","",people,facts,"score",2L).rows()).extracting(SeasonRecap.Standing::rank).containsExactly(1,1,3);
    }
    @Test void leaderboardEdgesShowTwoNeighboursAndHandleSmallTeams() {
        var people=new ArrayList<Map<String,Object>>();var facts=new ArrayList<Map<String,Object>>();
        for(long i=1;i<=7;i++){people.add(row("id",i,"name","Hráč "+i));facts.add(row("player_id",i,"score",10-i));}
        assertThat(SeasonRecapBuilder.board("","",people,facts,"score",1L).rows())
            .extracting(SeasonRecap.Standing::subjectId).containsExactly(1L,2L,3L);
        assertThat(SeasonRecapBuilder.board("","",people,facts,"score",7L).rows())
            .extracting(SeasonRecap.Standing::subjectId).containsExactly(1L,5L,6L,7L);
        assertThat(SeasonRecapBuilder.board("","",people.subList(0,2),facts,"score",2L).rows()).hasSize(2);
        assertThat(SeasonRecapBuilder.board("","",people.subList(0,1),facts,"score",1L).rows()).hasSize(1);
    }
    @Test void frequentFinesCountUnitsMergePriceVersionsAndKeepTopTen() {
        var fines=new ArrayList<Map<String,Object>>();
        fines.add(row("name","Kopačky","code","boots","count",14));
        fines.add(row("name","Kopačky","code","boots","count",5));
        for(int i=1;i<=12;i++)fines.add(row("name","Pokuta "+i,"code","fine"+i,"count",i));
        var board=SeasonRecapBuilder.frequentFines(fines);
        assertThat(board.rows()).hasSize(10);
        assertThat(board.rows().get(0).name()).isEqualTo("Kopačky");
        assertThat(board.rows().get(0).value()).isEqualTo(19);
        assertThat(board.rows()).extracting(SeasonRecap.Standing::value).containsExactly(19.0,12.0,11.0,10.0,9.0,8.0,7.0,6.0,5.0,4.0);
        assertThat(SeasonRecapBuilder.frequentFines(List.of()).rows()).isEmpty();
    }
    @Test void emptyDataIsSupported() {
        var empty=new SeasonRecapData.Facts(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        assertThat(builder.build("Test",LocalDate.now(),LocalDate.now(),null,1,empty).pages()).hasSize(9);
    }
}
