package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchTaskHelper;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class RetrievePkflMatchesByLeague {

    private final static String BASE_URL = "https://pkfl.cz";

    public List<FootballMatchTaskHelper> getMatches(LeagueDTO league) {

        List<FootballMatchTaskHelper> returnMatches = new ArrayList<>();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(league.getUri() + "?t=rozpis").get();

            Elements tables = document.getElementsByClass("dataTable table table-striped");
            for (int i = 0; i < tables.size(); i++) {
                //returnMatches.add(returnPkflMatch(tableElement, league.getId()));
                Elements trs = tables.get(i).select("tr");
                for (Element tr : trs) {
                    if (tr.select("th").isEmpty()) {
                        Elements tds = tr.select("td");
                        returnMatches.add(returnPkflMatch(tds, league, i+1));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnMatches;
    }

    private FootballMatchTaskHelper returnPkflMatch(Elements tds, LeagueDTO leagueDTO, int round) {
        FootballMatchTaskHelper pkflMatch = null;
        try {
            String date = tds.get(0).text().trim();
            String homeTeamUri = BASE_URL + tds.get(1).select("a[href]").attr("href");
            String awayTeamUri = BASE_URL + tds.get(2).select("a[href]").attr("href");
            String stadium = tds.get(3).text().trim();
            String referee = Objects.requireNonNull(tds.get(4).getElementsByClass("d-inline d-sm-none").first()).text().trim();
            String result = tds.get(5).text().trim();
            Integer homeGoalNumber = getScore(result, true);
            Integer awayGoalNumber = getScore(result, false);
            boolean alreadyPlayed = isAlreadyPlayed(homeGoalNumber, awayGoalNumber);
            String urlResult = BASE_URL + tds.get(5).select("a[href]").attr("href");
            pkflMatch = new FootballMatchTaskHelper(convertStringToDate(date, leagueDTO), homeTeamUri, awayTeamUri, round, leagueDTO, referee, stadium, homeGoalNumber, awayGoalNumber, urlResult, alreadyPlayed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkflMatch;
    }

    private Integer getScore(String result, boolean home) {
        String[] parts = result.split(":");
        if (parts.length < 2) {
            return null;
        }
        int firstNumber = Integer.parseInt(parts[0].trim());
        int secondNumber = Integer.parseInt(parts[1].trim());
        if (home) {
            return firstNumber;
        }
        return secondNumber;
    }

    private boolean isAlreadyPlayed(Integer homeGoalNumber, Integer awayGoalNumber) {
        return homeGoalNumber != null && awayGoalNumber != null;
    }

    private Date convertStringToDate(String dateTime, LeagueDTO league) {
        String[] dateTimeParts = dateTime.split("/");
        String datePart = dateTimeParts[0];
        String timePart = dateTimeParts[1];
        String dateTimeString = getYearOfMatchByLeagueAndDate(datePart, league) + "." + datePart + " " + timePart;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.dd.MM. HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        Date returnDate = new Date();
        try {
            returnDate = dateFormat.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return returnDate;
    }

    private String getYearOfMatchByLeagueAndDate(String date, LeagueDTO league) {
        String[] dateParts = date.split("\\.");
        int month = Integer.parseInt(dateParts[1].trim());
        String[] leagueParts = league.getYear().split("/");
        if (month < 8) {
            return leagueParts[1].trim();
        }
        return leagueParts[0].trim();
    }
}
