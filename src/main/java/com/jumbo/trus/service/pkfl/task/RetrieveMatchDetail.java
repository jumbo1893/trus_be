package com.jumbo.trus.service.pkfl.task;

import com.jumbo.trus.dto.pkfl.PkflIndividualStatsDTO;
import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.Jsoup;
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
            Element upperPart = document.getElementById("matchHero");
            Element refereePart = document.getElementById("matchArbitor");
            Element lineUpPart = document.getElementById("matchLineups");
            Elements commentSection = new Elements();
            if (refereePart != null) {
                commentSection = refereePart.select("p");
            }
            pkflMatch.setRefereeComment(getRefereeComment(commentSection));
            if (upperPart != null) {
                pkflMatch.setPlayerList(getPlayersFromMatch(getTrsFromTables(lineUpPart, isHomeMatch(upperPart)), pkflMatch));
            }
            else {
                pkflMatch.setPlayerList(new ArrayList<>());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pkflMatch;
    }

    private String getTextFromHtml(String html) {
        return Jsoup.parse(html).text();
    }

    private int getNumberOfImagesByAlt(Element html, String alt) {
        Elements elements = html.select("img[alt=" + alt + "]");
        return elements.size();
    }

    private String getRefereeComment(Elements ps) {
        if (ps.size() > 0 && ps.get(0).text().toLowerCase().contains("komentář")) {
            return getTextFromHtml(ps.get(0).text().trim());
        }
        else return "Bez komentáře rozhodčího";
    }

    private Boolean isHomeMatch(Element upperPart) {
        if (upperPart.getElementsByClass("team-name").size() == 0) {
            return false;
        }
        String name = upperPart.getElementsByClass("team-name").get(0).text().trim().toLowerCase();
        return name.equals("liščí trus");
    }

    private List<PkflIndividualStatsDTO> getPlayersFromMatch(Element trusLineup, PkflMatchDTO pkflMatch) {
        List<PkflIndividualStatsDTO> players = new ArrayList<>();
        Elements playerTrs = trusLineup.select("tr");
        for (Element playerTr : playerTrs) {
            Elements playerRows = playerTr.getElementsByClass("d-flex playerRow");
            if (!playerRows.isEmpty()) {
                players.add(initPlayerFromTds(playerRows.get(0), pkflMatch));
            }
        }
        return players;
    }

    private PkflIndividualStatsDTO initPlayerFromTds(Element playerRow, PkflMatchDTO pkflMatch) {
        PkflIndividualStatsDTO pkflMatchPlayer = null;
        try {
            PkflPlayerDTO pkflPlayerDTO = new PkflPlayerDTO();
            pkflPlayerDTO.setName(getPlayerName(playerRow));
            int goals = getNumberOfImagesByAlt(playerRow, "Gól");
            int ownGoals = getNumberOfImagesByAlt(playerRow, "Vlastní gól");
            int goalkeepingMinutes = getNumberOfGoalkeepingMinutes(playerRow);
            int receivedGoals = getNumberOfReceivedGoals(goalkeepingMinutes, pkflMatch.getOpponentGoalNumber());
            int yellowCards = getNumberOfYellowCards(playerRow);
            int redCards = getNumberOfRedCards(playerRow);
            boolean bestPlayer = isBestPlayer(playerRow);
            boolean hattrick = goals > 2;
            boolean cleanSheet = receivedGoals == 0 && goalkeepingMinutes > 0;
            String yellowCardComment = getCardComment(yellowCards, playerRow);
            String redCardComment = getCardComment(redCards, playerRow);
            pkflMatchPlayer = new PkflIndividualStatsDTO(pkflPlayerDTO, goals, receivedGoals, ownGoals, goalkeepingMinutes, yellowCards, redCards, bestPlayer, hattrick, cleanSheet, yellowCardComment, redCardComment);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pkflMatchPlayer;
    }

    private String getPlayerName(Element playerRow) {
        Element aQuery = playerRow.selectFirst("a");
        if (aQuery == null) {
            return "Neznámý hráč";
        }
        return aQuery.text().trim();
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

    private int getNumberOfReceivedGoals(int numberOfGoalkeepingMinutes, int trusReceivedGoals) {
        if (numberOfGoalkeepingMinutes > 0) {
            return trusReceivedGoals;
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

    private Element getTrsFromTables(Element lineUp, boolean homeMatch) {
        if (homeMatch) {
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
