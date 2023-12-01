package com.jumbo.trus.service.pkfl.task;

import com.jumbo.trus.dto.pkfl.PkflIndividualStatsDTO;
import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RetrieveMatchDetail {

    public PkflMatchDTO getMatchDetail(PkflMatchDTO pkflMatch) {
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(pkflMatch.getUrlResult()).get();
            Elements matches = document.getElementsByClass("matches");
            Elements ps = matches.select("p");
            Elements tables = document.getElementsByClass("dataTable table table-striped no-footer");
            pkflMatch.setRefereeComment(getRefereeComment(ps));
            pkflMatch.setPlayerList(getPlayersFromMatch(getTrsFromTables(tables, isHomeMatch(ps))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pkflMatch;
    }

    private String getRefereeComment(Elements ps) {
        if (ps.size() > 6 && ps.get(6).text().toLowerCase().contains("komentář")) {
            return ps.get(6).text().trim();
        }
        else return "Bez komentáře rozhodčího";
    }

    private Boolean isHomeMatch(Elements ps) {
        String name = ps.get(0).text().split("/")[1].trim().toLowerCase();
        return !name.equals("liščí trus");
    }

    private List<PkflIndividualStatsDTO> getPlayersFromMatch(Elements trs) {
        List<PkflIndividualStatsDTO> players = new ArrayList<>();

        for (Element tr : trs) {
            Elements tds = tr.select("td");
            if (tds.size() > 6) {
                players.add(initPlayerFromTds(tds));
            }
        }
        return players;
    }

    private PkflIndividualStatsDTO initPlayerFromTds(Elements tds) {
        PkflIndividualStatsDTO pkflMatchPlayer = null;
        try {
            PkflPlayerDTO pkflPlayerDTO = new PkflPlayerDTO();
            pkflPlayerDTO.setName(tds.get(0).text().trim());
            int goals = Integer.parseInt(tds.get(1).text().trim());
            int receivedGoals = Integer.parseInt(tds.get(2).text().trim());
            int ownGoals = Integer.parseInt(tds.get(3).text().trim());
            int goalkeepingMinutes =  getNumbersFromString(tds.get(4).text().trim());
            int yellowCards = Integer.parseInt(tds.get(5).text().trim());
            int redCards = Integer.parseInt(tds.get(6).text().trim());
            boolean bestPlayer = isBestPlayer(tds.get(0));
            boolean hattrick = goals > 2;
            boolean cleanSheet = receivedGoals == 0 && goalkeepingMinutes > 0;
            String yellowCardComment = getCardComment(yellowCards, tds.get(5));
            String redCardComment = getCardComment(redCards, tds.get(6));
            pkflMatchPlayer = new PkflIndividualStatsDTO(pkflPlayerDTO, goals, receivedGoals, ownGoals, goalkeepingMinutes, yellowCards, redCards, bestPlayer, hattrick, cleanSheet, yellowCardComment, redCardComment);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pkflMatchPlayer;
    }

    private String getCardComment(int cards, Element td) {
        String comment = null;
        if (cards > 0) {
            try {
                comment = td.select("a").first().attr("title");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return comment;
    }

    private int getNumbersFromString(String number) {
        return Integer.parseInt(number.replaceAll("[^0-9]", ""));
    }

    private Elements getTrsFromTables(Elements tables, boolean homeMatch) {
        if (homeMatch) {
            return tables.get(0).select("tr");
        }
        else {
            return tables.get(1).select("tr");
        }
    }

    private Boolean isBestPlayer(Element tdName) {
        Elements elements = tdName.getElementsByClass("best-player");
        return !elements.isEmpty();
    }
}
