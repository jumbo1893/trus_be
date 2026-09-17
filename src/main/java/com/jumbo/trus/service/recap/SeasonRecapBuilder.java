package com.jumbo.trus.service.recap;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.jumbo.trus.service.recap.SeasonRecap.*;

@Component
public class SeasonRecapBuilder {
    private static final Locale CS = Locale.forLanguageTag("cs-CZ");
    public SeasonRecap build(String name, LocalDate from, LocalDate to, Long player, long user, SeasonRecapData.Facts f) {
        var pages = new ArrayList<Page>();
        boolean personal = player != null && f.players().stream().anyMatch(p -> id(p,"id")==player);
        Long me = personal ? player : null;
        boolean footballer = personal && f.players().stream().anyMatch(p -> id(p,"id")==player && !Boolean.TRUE.equals(p.get("fan")));
        var ownDrinks = own(f.drinks(), me); var ownFines = own(f.fines(), me);
        var ownGoals = own(f.goals(), footballer ? me : null); var ownAwards = own(f.achievements(), me);
        var ownSteps = own(f.steps(), personal ? user : null); var ownFootbar = own(f.footbar(), footballer ? me : null);
        pages.add(new Page("intro", "Tohle byla sezona " + name,
                "Odehráli jsme " + f.matches().size() + " zápasů, zapsali " + fmt(sum(f.goals(),"goals")) + " gólů a rozdali "
                + f.achievements().size() + " achievementů. Co se povedlo, co se vypilo a co zůstalo v nohách?"
                + (personal ? " Tady je tvůj sezonní příběh." : " Nemáš propojeného hráče — ukážeme ti příběh celého týmu."),
                List.of(new Metric("Období aktivity", displayDate(from) + " – " + displayDate(to))), List.of()));
        pages.add(page("drinks", "Na zdraví, čus Trus!", "Piva a panáky vypitá v průběhu sezony",
                metrics(me, ownDrinks, f.drinks(), "beers", "Piva", "shots", "Panáky"),
                List.of(board("Žebříček piv", "piv", f.players(), f.drinks(), "beers", me), board("Žebříček panáků", "panáků", f.players(), f.drinks(), "shots", me))));
        var bestDrinks = new ArrayList<Metric>();
        bestMatch(bestDrinks, "Tvůj pivní rekord padl v zápase", personal ? ownDrinks : f.drinks(), f.drinks(), "beers", f.matches());
        bestMatch(bestDrinks, "Nejvíce panáků jsi vypil v zápase", personal ? ownDrinks : f.drinks(), f.drinks(), "shots", f.matches());
        pages.add(page("drink_match", "Večer, na který se nezapomíná", personal ? "Tvoje maxima a celkový splávek v zápasech." : "Největší týmové večery.", bestDrinks, List.of()));
        var fineMetrics = metrics(me, ownFines, f.fines(), "amount", "Pokuty (Kč)", "count", "Počet pokut");
        var biggest = (personal ? ownFines : f.fines()).stream().max(Comparator.comparingDouble(r -> num(r,"unit_amount")));
        biggest.ifPresent(r -> fineMetrics.add(new Metric("Nejvyšší jednotlivě udělená pokuta", r.get("name") + " · " + fmt(num(r,"unit_amount")) + " Kč · " + matchName(f.matches(), id(r,"match_id")))));
        pages.add(page("fines", "Pokladna nezahálela", "Částky jsou za udělené pokuty v sezoně", fineMetrics,
                List.of(board("Žebříček podle částky", "Kč", f.players(), f.fines(), "amount", me), frequentFines(f.fines()))));
        var goalMetrics = metrics(footballer ? me : null, ownGoals, f.goals(), "goals", "Góly", "assists", "Asistence");
        var points = f.goals().stream().map(r -> {var copy = new HashMap<>(r); copy.put("points",num(r,"goals")+num(r,"assists")); return (Map<String,Object>)copy;}).toList();
        bestMatch(goalMetrics, "Nejvíc kanadských bodů", footballer ? own(points,me) : points, points, "points", f.matches());
        var footballPlayers = f.players().stream().filter(p -> !Boolean.TRUE.equals(p.get("fan"))).toList();
        pages.add(page("goals", "Góly, asistence", "Hoši, já potřebuju nějaké kanadské body, kurva", goalMetrics,
                List.of(board("Střelci", "gólů", footballPlayers, f.goals(), "goals", footballer ? me : null), board("Asistenti", "asistencí", footballPlayers, f.goals(), "assists", footballer ? me : null))));
        var awardMetrics = new ArrayList<Metric>();
        if (personal) awardMetrics.add(new Metric("Tvoje nové achievementy", ""+ownAwards.size()));
        awardMetrics.add(new Metric("Celkem uděleno v týmu", ""+f.achievements().size()));
        (personal ? ownAwards : f.achievements()).stream().sorted(Comparator.<Map<String,Object>>comparingDouble(r -> num(r,"holders")/Math.max(1,num(r,"eligible"))).thenComparing(r -> String.valueOf(r.get("name"))))
                .collect(Collectors.toMap(r -> id(r,"achievement_id"), r -> r, (a,b)->a, LinkedHashMap::new)).values().stream().limit(3)
                .forEach(r -> awardMetrics.add(new Metric("Vzácný achievement", r.get("name") + " · splnilo " + fmt(num(r,"holders")) + "/" + fmt(num(r,"eligible")) + " hráčů")));
        var awardCounts = f.achievements().stream().map(r -> {var c=new HashMap<>(r);c.put("awards",1);return (Map<String,Object>)c;}).toList();
        pages.add(page("achievements", "Síň slávy", "Souhrn pro sběratele odznáčků.", awardMetrics,
                List.of(board("Získané achievementy", "ocenění", f.players(), awardCounts,"awards", me))));
        var stepMetrics = new ArrayList<Metric>();
        if (!ownSteps.isEmpty()) {
            stepMetrics.add(new Metric("Tvoje kroky",fmt(sum(ownSteps,"steps"))));
            ownSteps.stream().max(Comparator.comparingDouble(r->num(r,"steps"))).ifPresent(r -> stepMetrics.add(new Metric("Tvůj nejaktivnější den",r.get("step_date")+" · "+fmt(num(r,"steps"))+" kroků")));
        }
        stepMetrics.add(new Metric("Celkem sdílených kroků týmu",fmt(sum(f.steps(),"steps"))));
        var walkers = f.steps().stream().map(r -> Map.<String,Object>of("id",r.get("player_id"),"name",r.get("name"))).distinct().toList();
        pages.add(page("steps", "Každý krok se počítá", displayDate(from)+" – "+displayDate(to)+". Pouze uživatelé se zapnutým sdílením kroků.", stepMetrics,
                List.of(board("Tým na nohou", "kroků", walkers, f.steps(),"steps",personal ? user : null))));
        var footMetrics = metrics(ownFootbar.isEmpty()?null:me, ownFootbar, f.footbar(),"shots","Střely","passes","Přihrávky");
        if (!ownFootbar.isEmpty()) {
            footMetrics.add(new Metric("Tvoje naběhaná vzdálenost",fmt(sum(ownFootbar,"km"))+" km"));
            footMetrics.add(new Metric("Tvoje nejrychlejší střela",fmt(max(ownFootbar,"speed"))+" km/h"));
        }
        footMetrics.add(new Metric("Naběháno v týmu",fmt(sum(f.footbar(),"km"))+" km"));
        footMetrics.add(new Metric("Nejrychlejší střela týmu",fmt(max(f.footbar(),"speed"))+" km/h"));
        var measuredPlayers = footballPlayers.stream().filter(p -> f.footbar().stream().anyMatch(s -> id(s,"player_id")==id(p,"id"))).toList();
        pages.add(page("footbar", "Řečí Footbaru", "Kdo byl nejlepší hráč ze všech? Kdo nemá footbar, tak na to nemůže ani myslet!",footMetrics,
                List.of(board("Naběhané kilometry","km",measuredPlayers,f.footbar(),"km",ownFootbar.isEmpty()?null:me))));
        var attendance = f.attendance().stream().map(r->{var c=new HashMap<>(r);c.put("visits",1);return (Map<String,Object>)c;}).toList();
        var visitMetrics = new ArrayList<Metric>();
        if(personal) visitMetrics.add(new Metric("Tvoje účasti",fmt(sum(own(attendance,me),"visits"))));
        visitMetrics.add(new Metric("Počet zápasů", ""+f.matches().size()));
        var visitsByMatch = totals(attendance,"match_id","visits");
        Comparator<Map<String,Object>> cmp=Comparator.comparingDouble(m->visitsByMatch.getOrDefault(id(m,"id"),0.0));
        f.matches().stream().max(cmp).ifPresent(m->visitMetrics.add(new Metric("Nejvyšší účast",matchName(f.matches(),id(m,"id"))+" · "+fmt(visitsByMatch.getOrDefault(id(m,"id"),0.0))+" lidí")));
        f.matches().stream().min(cmp).ifPresent(m->visitMetrics.add(new Metric("Nejnižší účast",matchName(f.matches(),id(m,"id"))+" · "+fmt(visitsByMatch.getOrDefault(id(m,"id"),0.0))+" lidí")));
        pages.add(page("attendance","Bez tebe by to nebylo ono","Účast podle zapsané sestavy zápasu, včetně fanoušků",visitMetrics,
                List.of(board("Docházka","účastí",f.players(),attendance,"visits",me))));
        return new SeasonRecap(name,from.toString(),to.toString(),List.copyOf(pages));
    }
    private Page page(String kind,String title,String text,List<Metric> metrics,List<Board> boards) {return new Page(kind,title,text,metrics,boards);}
    private ArrayList<Metric> metrics(Long me,List<Map<String,Object>> own,List<Map<String,Object>> all,String key,String label,String key2,String label2) {
        var result=new ArrayList<Metric>();
        if(me!=null){result.add(new Metric("Ty · "+label,fmt(sum(own,key))));result.add(new Metric("Ty · "+label2,fmt(sum(own,key2))));}
        result.add(new Metric("Tým · "+label,fmt(sum(all,key))));result.add(new Metric("Tým · "+label2,fmt(sum(all,key2))));return result;
    }
    private void bestMatch(List<Metric> result,String label,List<Map<String,Object>> own,List<Map<String,Object>> all,String key,List<Map<String,Object>> matches) {
        var totals=totals(own,"match_id",key);
        totals.entrySet().stream().filter(e->e.getValue()>0).sorted(Map.Entry.<Long,Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).findFirst().ifPresent(e -> {
            result.add(new Metric(label,matchName(matches,e.getKey())+" · "+fmt(e.getValue())));
            String unit = switch(key) {case "beers" -> "piv"; case "shots" -> "panáků"; case "points" -> "kanadských bodů"; default -> "";};
            result.add(new Metric("Celkem v tomto zápase",fmt(totals(all,"match_id",key).getOrDefault(e.getKey(),0.0))+" "+unit));
            if(key.equals("beers")||key.equals("shots")) {
                String other=key.equals("beers")?"shots":"beers";
                result.add(new Metric(key.equals("beers")?"A k tomu panáků:":"A k tomu piv:",fmt(totals(all,"match_id",other).getOrDefault(e.getKey(),0.0))));
            }
        });
        if(totals.values().stream().noneMatch(v->v>0)) result.add(new Metric(label,"Zatím bez záznamu"));
    }
    public static Board board(String title,String unit,List<Map<String,Object>> people,List<Map<String,Object>> facts,String key,Long me) {
        var totals=totals(facts,"player_id",key);
        var sorted=people.stream().sorted(Comparator.<Map<String,Object>>comparingDouble(p->totals.getOrDefault(id(p,"id"),0.0)).reversed().thenComparing(p->String.valueOf(p.get("name"))).thenComparingLong(p->id(p,"id"))).toList();
        int mine=-1; for(int i=0;i<sorted.size();i++) if(me!=null&&id(sorted.get(i),"id")==me) mine=i;
        var selected=new TreeSet<Integer>(); if(!sorted.isEmpty())selected.add(0);
        if(mine>=0){
            int start=Math.max(0,Math.min(mine-1,sorted.size()-3));
            for(int i=start;i<Math.min(sorted.size(),start+3);i++)selected.add(i);
        }
        else for(int i=0;i<Math.min(3,sorted.size());i++)selected.add(i);
        var rows=new ArrayList<Standing>(); double previous=Double.NaN;int rank=0;
        for(int i=0;i<sorted.size();i++) {var p=sorted.get(i);long id=id(p,"id");double value=totals.getOrDefault(id,0.0);if(Double.compare(value,previous)!=0)rank=i+1;previous=value;
            if(selected.contains(i))rows.add(new Standing(id,String.valueOf(p.get("name")),rank,value,me!=null&&id==me));}
        return new Board(title,unit,rows);
    }
    static Board frequentFines(List<Map<String,Object>> fines) {
        // Amount versions share a stable code; count actual fine units, not DB rows.
        var counts=new HashMap<String,Double>();
        var names=new HashMap<String,String>();
        for(var fine:fines) {
            String name=String.valueOf(fine.get("name"));
            String key=fine.get("code") instanceof String code && !code.isBlank()?"code:"+code:"name:"+name;
            counts.merge(key,num(fine,"count"),Double::sum);
            names.merge(key,name,(a,b)->a.compareTo(b)<=0?a:b);
        }
        var sorted=counts.entrySet().stream().filter(e->e.getValue()>0)
                .sorted(Map.Entry.<String,Double>comparingByValue().reversed()
                    .thenComparing(e->names.get(e.getKey())).thenComparing(Map.Entry::getKey))
                .limit(10).toList();
        var rows=new ArrayList<Standing>();
        double previous=Double.NaN;int rank=0;
        for(int i=0;i<sorted.size();i++) {
            var item=sorted.get(i);
            if(Double.compare(previous,item.getValue())!=0)rank=i+1;
            previous=item.getValue();
            rows.add(new Standing(i+1,names.get(item.getKey()),rank,item.getValue(),false));
        }
        return new Board("Nejčastější pokuty v týmu","udělení",rows);
    }
    static Map<Long,Double> totals(List<Map<String,Object>> rows,String id,String key) {var result=new HashMap<Long,Double>();for(var r:rows)result.merge(id(r,id),num(r,key),Double::sum);return result;}
    static List<Map<String,Object>> own(List<Map<String,Object>> rows,Long id){return id==null?List.of():rows.stream().filter(r->id(r,"player_id")==id).toList();}
    static long id(Map<String,Object> row,String key){return ((Number)row.getOrDefault(key,0L)).longValue();}
    static double num(Map<String,Object> row,String key){return row.get(key) instanceof Number n?n.doubleValue():0;}
    static double sum(List<Map<String,Object>> rows,String key){return rows.stream().mapToDouble(r->num(r,key)).sum();}
    static double max(List<Map<String,Object>> rows,String key){return rows.stream().mapToDouble(r->num(r,key)).max().orElse(0);}
    static String fmt(double n){return String.format(CS,n==Math.rint(n)?"%,.0f":"%,.1f",n);}
    static LocalDate date(Object d){if(d instanceof java.sql.Date s)return s.toLocalDate();return Instant.ofEpochMilli(((java.util.Date)d).getTime()).atZone(ZoneId.of("Europe/Prague")).toLocalDate();}
    static String displayDate(LocalDate d){return d.format(DateTimeFormatter.ofPattern("d. M. yyyy"));}
    static String matchName(List<Map<String,Object>> matches,long id){return matches.stream().filter(m->id(m,"id")==id).findFirst().map(m->m.get("name")+" · "+displayDate(date(m.get("date")))).orElse("Zápas #"+id);}
}
