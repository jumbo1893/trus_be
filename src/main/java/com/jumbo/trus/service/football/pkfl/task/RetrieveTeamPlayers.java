package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class RetrieveTeamPlayers {

    private final static String BASE_URL = "https://pkfl.cz";

    private final LoginToPkfl loginToPkfl;


    public List<FootballPlayerDTO> getFootballers(TeamDTO teamDTO) {
        List<FootballPlayerDTO> footballers = new ArrayList<>();
        try {
            log.debug("Zpracovávám tým {}", teamDTO.getName());
            Connection.Response response = loginToPkfl.getLoggedAccessToPkflWeb(teamDTO.getUri());
            log.debug("načtena URL");
            Document document = response.parse();
            Element playerTable = Objects.requireNonNull(document.getElementById("soupiska"));
            Elements players = playerTable.select("tr");
            for (Element player : players) {
                Elements tds = player.select("td");
                if (tds.size() > 1) {
                    String name = tds.get(0).text().trim();
                    String uri = BASE_URL + tds.get(0).select("a[href]").attr("href");
                    int yearBirthday = -1;
                    try {
                        yearBirthday = Integer.parseInt(tds.get(1).text().trim());
                    } catch (NumberFormatException e) {
                        System.out.println("U hráče " + name + " v týmu " + teamDTO.getName() + " není datum narození, zapisuji -1");
                    }
                    List<Long> teamIdList = new ArrayList<>();
                    teamIdList.add(teamDTO.getId());
                    FootballPlayerDTO footballPlayerDTO = new FootballPlayerDTO(teamIdList, name, yearBirthday, null, null, uri);
                    footballers.add(footballPlayerDTO);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return footballers;
    }
}
