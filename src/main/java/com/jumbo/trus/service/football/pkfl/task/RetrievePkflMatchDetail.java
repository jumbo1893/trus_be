package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchDetailTaskHelper;
import com.jumbo.trus.service.football.pkfl.task.helper.PlayerMatchStatsHelper;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RetrievePkflMatchDetail {

    public static final String NO_REFEREE_COMMENT = "Bez komentáře rozhodčího";
    private final static String BASE_URL = "https://pkfl.cz";

    public FootballMatchDetailTaskHelper getMatchDetail(FootballMatchDTO footballMatchDTO) {
        FootballMatchDetailTaskHelper detail = new FootballMatchDetailTaskHelper();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(footballMatchDTO.getUrlResult()).get();
            Element upperPart = document.getElementById("matchHero");
            Element refereePart = document.getElementById("matchArbitor");
            Element lineUpPart = document.getElementById("matchLineups");
            Elements commentSection = new Elements();
            if (refereePart != null) {
                commentSection = refereePart.select("p");
            }
            detail.setRefereeComment(getRefereeComment(commentSection));
            if (upperPart != null) {
                detail.setHomeTeamPlayers(getPlayersFromMatch(getTrsFromTables(lineUpPart, true), footballMatchDTO.getAwayGoalNumber()));
                detail.setAwayTeamPlayers(getPlayersFromMatch(getTrsFromTables(lineUpPart, false), footballMatchDTO.getAwayGoalNumber()));
            }
            else {
                detail.setHomeTeamPlayers(new ArrayList<>());
                detail.setAwayTeamPlayers(new ArrayList<>());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return detail;
    }

    private String getTextFromHtml(String html) {
        return Jsoup.parse(html).text();
    }

    private int getNumberOfImagesByAlt(Element html, String alt) {
        Elements elements = html.select("img[alt=" + alt + "]");
        return elements.size();
    }

    private String getRefereeComment(Elements ps) {
        if (!ps.isEmpty() && ps.get(0).text().toLowerCase().contains("komentář")) {
            return getTextFromHtml(ps.get(0).text().trim());
        }
        else return NO_REFEREE_COMMENT;
    }

    private Boolean isHomeMatch(Element upperPart) {
        if (upperPart.getElementsByClass("team-name").isEmpty()) {
            return false;
        }
        String name = upperPart.getElementsByClass("team-name").get(0).text().trim().toLowerCase();
        return name.equals("liščí trus");
    }

    private List<PlayerMatchStatsHelper> getPlayersFromMatch(Element teamLineup, int opponentGoalNumber) {
        List<PlayerMatchStatsHelper> players = new ArrayList<>();
        Elements playerTrs = teamLineup.select("tr");
        for (Element playerTr : playerTrs) {
            Elements playerRows = playerTr.getElementsByClass("d-flex playerRow");
            if (!playerRows.isEmpty()) {
                players.add(initPlayerFromTds(playerRows.get(0), opponentGoalNumber));
            }
        }
        return players;
    }

    private PlayerMatchStatsHelper initPlayerFromTds(Element playerRow, int opponentGoalNumber) {
        PlayerMatchStatsHelper playerMatchStatsHelper = null;
        try {

            String playerUri = getPlayerUri(playerRow);
            int goals = getNumberOfImagesByAlt(playerRow, "Gól");
            int ownGoals = getNumberOfImagesByAlt(playerRow, "Vlastní gól");
            int goalkeepingMinutes = getNumberOfGoalkeepingMinutes(playerRow);
            int receivedGoals = getNumberOfReceivedGoals(goalkeepingMinutes, opponentGoalNumber);
            int yellowCards = getNumberOfYellowCards(playerRow);
            int redCards = getNumberOfRedCards(playerRow);
            boolean bestPlayer = isBestPlayer(playerRow);
            boolean hattrick = goals > 2;
            boolean cleanSheet = receivedGoals == 0 && goalkeepingMinutes > 0;
            String yellowCardComment = getCardComment(yellowCards, playerRow);
            String redCardComment = getCardComment(redCards, playerRow);
            playerMatchStatsHelper = new PlayerMatchStatsHelper(playerUri, goals, receivedGoals, ownGoals, goalkeepingMinutes, yellowCards, redCards, bestPlayer, hattrick, cleanSheet, yellowCardComment, redCardComment);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return playerMatchStatsHelper;
    }

    private String getPlayerUri(Element playerRow) {
        Element aQuery = playerRow.selectFirst("a");
        if (aQuery == null) {
            return "Neznámý hráč";
        }
        return BASE_URL + aQuery.attr("href");
    }
    private String getCardComment(int cards, Element playerRow) {
        String comment = null;
        if (cards > 0) {
            try {

                Element cardElement = playerRow.selectFirst("a[data-toggle=tooltip]");
                if (cardElement != null) {
                    comment = cardElement.attr("title");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return comment;
    }

    private int getNumberOfReceivedGoals(int numberOfGoalkeepingMinutes, int receivedGoals) {
        if (numberOfGoalkeepingMinutes > 0) {
            return receivedGoals;
        }
        return 0;
    }

    private int getNumberOfGoalkeepingMinutes(Element playerRow) {
        if (getNumberOfImagesByAlt(playerRow, "Chytal") > 0) {
            return getNumbersFromString(playerRow.text().trim());
        }
        return 0;
    }

    private int getNumbersFromString(String number) {
        if (number == null || number.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(number.replaceAll("[^0-9]", ""));
    }

    private Element getTrsFromTables(Element lineUp, boolean homeTeam) {
        if (homeTeam) {
            return lineUp.getElementsByClass("col-md-6 pr-md-3").get(0);
        }
        else {
            return lineUp.getElementsByClass("col-md-6 pl-md-3").get(0);
        }
    }

    private Boolean isBestPlayer(Element playerRow) {
        Elements bestPlayerSvg = playerRow.select("svg path[fill=\"#FFD700\"]");
        return !bestPlayerSvg.isEmpty();
    }

    private Integer getNumberOfYellowCards(Element playerRow) {
        Elements yellowCardSvg = playerRow.select("svg rect[fill=\"#FFD700\"]");
        return yellowCardSvg.size();
    }

    private Integer getNumberOfRedCards(Element playerRow) {
        Elements redCardSvg = playerRow.select("svg rect[fill=\"#FF0000\"]");
        return redCardSvg.size();
    }
}
