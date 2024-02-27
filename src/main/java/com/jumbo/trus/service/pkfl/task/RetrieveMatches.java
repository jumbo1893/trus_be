package com.jumbo.trus.service.pkfl.task;

import com.jumbo.trus.dto.pkfl.*;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class RetrieveMatches {

    public List<PkflMatchDTO> getMatches(PkflSeasonDTO season) {
        List<PkflMatchDTO> returnMatches = new ArrayList<>();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(season.getUrl()).get();

            Elements table = document.getElementsByClass("dataTable table table-bordered table-striped");
            Elements trs = table.select("tr");
            for (Element tr : trs) {
                Elements tds = tr.select("td");
                if (tds.size() > 8) {

                    returnMatches.add(returnPkflMatch(tds, season));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnMatches;
    }

    private PkflMatchDTO returnPkflMatch(Elements tds, PkflSeasonDTO season) {
        PkflMatchDTO pkflMatch = null;
        try {
            String BASE_URL = "https://pkfl.cz";
            String date = tds.get(0).text().trim();
            String time = tds.get(1).text().trim();
            String homeTeam = tds.get(4).text().trim();
            String awayTeam = tds.get(5).text().trim();
            int round = Integer.parseInt(tds.get(2).text().trim());
            String league = tds.get(3).text().trim();
            PkflStadiumDTO stadium = new PkflStadiumDTO();
            stadium.setName(tds.get(6).text().trim());
            PkflRefereeDTO referee = new PkflRefereeDTO();
            referee.setName(tds.get(7).text().trim());
            String result = tds.get(8).text().trim();
            Integer trusGoalNumber = getScore(homeTeam, result, true);
            Integer opponentGoalNumber = getScore(homeTeam, result, false);
            boolean alreadyPlayed = isAlreadyPlayed(trusGoalNumber, opponentGoalNumber);
            String urlResult = BASE_URL + tds.get(8).select("a[href]").attr("href");
            pkflMatch = new PkflMatchDTO(convertStringToDate(date, time), getOpponent(homeTeam, awayTeam), round,
                    league, stadium, referee, season, trusGoalNumber, opponentGoalNumber, isHomeMatch(homeTeam), urlResult, alreadyPlayed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkflMatch;
    }

    private Integer getScore(String homeTeam, String result, boolean trus) {
        String[] parts = result.split(":");
        if (parts.length < 2) {
            return null;
        }
        int firstNumber = Integer.parseInt(parts[0].trim());
        int secondNumber = Integer.parseInt(parts[1].trim());
        if ((isHomeMatch(homeTeam) && trus) || (!isHomeMatch(homeTeam) && !trus)) {
            return firstNumber;
        }
        return secondNumber;
    }

    private boolean isAlreadyPlayed(Integer trusGoalNumber, Integer opponentGoalNumber) {
        return trusGoalNumber != null && opponentGoalNumber != null;
    }

    private PkflOpponentDTO getOpponent(String homeTeam, String awayTeam) {
        if (homeTeam.trim().equals("Liščí trus")) {
            PkflOpponentDTO opponent = new PkflOpponentDTO();
            opponent.setName(awayTeam);
            return opponent;
        } else {
            PkflOpponentDTO opponent = new PkflOpponentDTO();
            opponent.setName(homeTeam);
            return opponent;
        }
    }

    private boolean isHomeMatch(String homeTeam) {
        return homeTeam.trim().equals("Liščí trus");
    }

    private Date convertStringToDate(String date, String time) {
        String dateTimeString = date + " " + time;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        Date returnDate = new Date();
        try {
            returnDate = dateFormat.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return returnDate;
    }
}
