package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.pkfl.PkflSeasonDTO;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RetrieveSeasonUrl {

    public List<PkflSeasonDTO> getSeasonUrls(String trusUrl) {
        List<PkflSeasonDTO> returnSeasons = new ArrayList<>();
        try {
            //Connect to the website
            LoginToPkfl loginToPkfl = new LoginToPkfl();
            Connection.Response res = loginToPkfl.getLoggedAccessToPkflWeb(trusUrl);
            Document document = res.parse();
            Element matchesSpinnerDiv = document.getElementsByClass("dropdown-content").get(0);
            Elements spinnerSeason = matchesSpinnerDiv.select("a[href]");


            for (Element spinnerButton : spinnerSeason) {
                returnSeasons.add(returnPkflSeason(spinnerButton));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnSeasons;
    }

    public List<PkflSeasonDTO> getCurrentSeasonUrls(String trusUrl, String currentSeasonText) {
        List<PkflSeasonDTO> returnSeasons = new ArrayList<>();
        try {
            //Connect to the website
            Document document = SSLHelper.getConnection(trusUrl).get();

            Element matchesSpinnerDiv = document.getElementsByClass("dropdown-content").get(0);
            Elements spinnerSeason = matchesSpinnerDiv.select("a[href]");


            for (Element spinnerButton : spinnerSeason) {
                if (returnPkflSeason(spinnerButton).getName().contains(currentSeasonText)) {
                    returnSeasons.add(returnPkflSeason(spinnerButton));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnSeasons;
    }

    private PkflSeasonDTO returnPkflSeason(Element spinnerButton) {
        PkflSeasonDTO pkflSeason = null;
        try {
            String BASE_URL = "https://pkfl.cz";
            pkflSeason = new PkflSeasonDTO();
            pkflSeason.setUrl(BASE_URL + spinnerButton.select("a[href]").attr("href"));
            pkflSeason.setName(spinnerButton.text().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pkflSeason;
    }
}
