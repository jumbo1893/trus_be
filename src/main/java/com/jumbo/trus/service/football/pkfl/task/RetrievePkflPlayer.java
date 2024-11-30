package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.service.task.SSLHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RetrievePkflPlayer {

    public FootballPlayerDTO getPlayer(String uri, Long teamId) {
        FootballPlayerDTO footballer = new FootballPlayerDTO();
        footballer.setUri(uri);
        List<Long> teamIdList = new ArrayList<>();
        if (teamId != null) {
            teamIdList.add(teamId);
        }
        footballer.setTeamIdList(teamIdList);
        try {
            Document document = SSLHelper.getConnection(uri).get();
            Element nameElement = document.selectFirst("dt[itemprop=name]");
            Element birthYearElement = document.selectFirst("dt[itemprop=birthDate]");
            Element mailElement = document.selectFirst("dt[itemprop=email]");
            Element phoneElement = document.selectFirst("dt[itemprop=phone]");
            footballer.setName(getTextFromElement(nameElement));
            int yearBirthday = -1;
            try {
                yearBirthday = Integer.parseInt(getTextFromElement(birthYearElement));
            } catch (NumberFormatException e) {
                System.out.println("U hráče " + footballer.getName() + " v týmu id" + teamId + " není datum narození, zapisuji -1");
            }
            footballer.setBirthYear(yearBirthday);
            footballer.setEmail(getTextFromElement(mailElement));
            footballer.setPhoneNumber(getTextFromElement(phoneElement));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return footballer;
    }

    private String getTextFromElement(Element element) {
        return Objects.requireNonNull(element).text();
    }
}
