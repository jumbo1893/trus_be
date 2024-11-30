package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RetrievePkflLeagues {

    private static final String PKFL_LEAGUE_URL = "https://pkfl.cz/liga/";
    private static final String BASE_URL = "https://pkfl.cz";


    public List<LeagueDTO> getLeagues(Element yearElement) {
        List<LeagueDTO> returnLeagues = new ArrayList<>();
        try {
            //Connect to the website
            Document document;
            if (yearElement == null) {
                document = SSLHelper.getConnection(PKFL_LEAGUE_URL).get();
                yearElement = getYearElement(document);
            }
            else {
                document = SSLHelper.getConnection(BASE_URL + yearElement.select("a[href]").attr("href")).get();
            }
            for (Element spinnerButton : getLeagueSpinner(document)) {
                returnLeagues.add(returnPkflLeague(spinnerButton, yearElement, document.baseUri().equals(PKFL_LEAGUE_URL)));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnLeagues;
    }

    public List<LeagueDTO> getAllPastLeagues() {
        List<LeagueDTO> returnLeagues = new ArrayList<>();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(PKFL_LEAGUE_URL).get();
            for (Element spinnerButton : getYearSpinner(document)) {
                returnLeagues.addAll(getLeagues(spinnerButton));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnLeagues;
    }

    private LeagueDTO returnPkflLeague(Element spinnerButton, Element spinnerYear, boolean currentLeague) {
        LeagueDTO league = null;
        try {
            league = new LeagueDTO();
            league.setUri(BASE_URL + spinnerButton.select("a[href]").attr("href"));
            league.setName(spinnerButton.text().trim());
            league.setRank(returnLeagueRankFromName(league.getName()));
            league.setOrganization(Organization.PKFL);
            league.setOrganizationUnit("PKFL");
            league.setYear(spinnerYear.text());
            league.setCurrentLeague(currentLeague);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return league;
    }

    private int returnLeagueRankFromName(String name) {
        Matcher m = Pattern.compile("[^0-9]*([0-9]+).*").matcher(name);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private Elements getLeagueSpinner(Document document) {
        return document.getElementsByClass("league-title").get(2).firstElementSibling().getElementsByClass("dropdown-content").get(0).select("a[href]");
    }

    private Element getYearElement(Document document) {
        return document.getElementsByClass("league-title").get(0).firstElementSibling().getElementsByClass("dropdown-content").get(0).selectFirst("a[href]");
    }

    private Element getYearElement(Document document, String uri) {
        return document.getElementsByClass("league-title").get(0).firstElementSibling().getElementsByClass("dropdown-content").get(0).selectFirst("a[href=(" + uri + ")]");
    }

    private Elements getYearSpinner(Document document) {
        return document.getElementsByClass("league-title").get(0).firstElementSibling().getElementsByClass("dropdown-content").get(0).select("a[href]");
    }
}
