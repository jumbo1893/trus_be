package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.pkfl.*;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class RetrievePkflTable {

    public List<PkflTableTeamDTO> getTableTeams(String tableUrl) {
        List<PkflTableTeamDTO> returnTeams = new ArrayList<>();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(tableUrl).get();

            Element table = document.getElementById("grounds");
            assert table != null;
            Elements trs = table.select("tr");
            for (Element tr : trs) {
                Elements tds = tr.select("td");
                if (tds.size() > 6) {
                    returnTeams.add(returnPkflTeam(tds));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnTeams;
    }

    private PkflTableTeamDTO returnPkflTeam(Elements tds) {
        PkflTableTeamDTO pkflTeam = null;
        try {
            String longName = tds.get(1).getElementsByClass("d-inline d-md-none").text().trim();
            int rank = Integer.parseInt((tds.get(0).text().trim()));
            int matches = Integer.parseInt((tds.get(2).text().trim()));
            String winRatio = tds.get(3).text().trim();
            String score = tds.get(4).text().trim();
            String penalty = tds.get(5).text().trim();
            int points = 0;
            try {
                points = Integer.parseInt((tds.get(6).text().trim()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            pkflTeam = new PkflTableTeamDTO(new PkflOpponentDTO(-1, longName), rank, matches, getRatio(winRatio, WinDrawLose.WIN), getRatio(winRatio, WinDrawLose.DRAW), getRatio(winRatio, WinDrawLose.LOSE),
                    getScore(score, true), getScore(score, false), penalty, points, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkflTeam;
    }

    private Integer getRatio(String ratio, WinDrawLose winDrawLose) {
        try {
            String[] parts = ratio.split("/");
            if (parts.length < 3) {
                return 0;
            }
            switch (winDrawLose) {
                case WIN -> {
                    return Integer.parseInt(parts[0].trim());
                }
                case DRAW -> {
                    return Integer.parseInt(parts[1].trim());
                }
                case LOSE -> {
                    return Integer.parseInt(parts[2].trim());
                }
                default -> {
                    return 0;
                }
            }
        }
        catch (Exception e) {
            return 0;
        }
    }

    private Integer getScore(String score, boolean scoredGoals) {
        try {
            String[] parts = score.split(":");
            if (parts.length < 2) {
                return 0;
            }
            if (scoredGoals) {
                return Integer.parseInt(parts[0].trim());
            }
            return Integer.parseInt(parts[1].trim());
        }
        catch (Exception e) {
            return 0;
        }
    }

    enum WinDrawLose {
        WIN, DRAW, LOSE
    }
}
