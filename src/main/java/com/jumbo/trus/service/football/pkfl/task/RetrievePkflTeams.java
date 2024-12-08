package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import com.jumbo.trus.service.football.helper.WinDrawLose;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RetrievePkflTeams {

    private final static String BASE_URL = "https://pkfl.cz";

    public List<TeamTableTeam> getTeams(List<LeagueDTO> leagues) {
        List<TeamTableTeam> returnTeamTableTeams = new ArrayList<>();
        try {
            for(LeagueDTO league : leagues) {

                Document document = SSLHelper.getConnection(league.getUri() + "?t=prubez").get();

                Element table = document.getElementById("grounds");
                assert table != null;
                Elements trs = table.select("tr");
                for (Element tr : trs) {
                    Elements tds = tr.select("td");
                    if (tds.size() > 6) {
                        returnTeamTableTeams.add(new TeamTableTeam(returnTeam(tds, league), returnTableTeam(tds, league)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnTeamTableTeams;
    }

    private TeamDTO returnTeam(Elements tds, LeagueDTO leagueDTO) {
        TeamDTO pkflTeam = null;
        try {
            Element nameElement = tds.get(1);
            String longName = nameElement.getElementsByClass("d-inline d-md-none").text().trim();
            String uri = BASE_URL + nameElement.select("a[href]").attr("href");
            pkflTeam = new TeamDTO(longName, leagueDTO.getId(), uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkflTeam;
    }

    private TableTeamDTO returnTableTeam(Elements tds, LeagueDTO leagueDTO) {
        TableTeamDTO tableTeamDTO = null;
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
            } catch (NumberFormatException e) {
                System.out.println("U týmu " + longName + " není počet bodů, zapisuji 0");
            }

            tableTeamDTO = new TableTeamDTO(rank, matches, getRatio(winRatio, WinDrawLose.WIN), getRatio(winRatio, WinDrawLose.DRAW), getRatio(winRatio, WinDrawLose.LOSE),
                    getScore(score, true), getScore(score, false), penalty, points, leagueDTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableTeamDTO;
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
}
