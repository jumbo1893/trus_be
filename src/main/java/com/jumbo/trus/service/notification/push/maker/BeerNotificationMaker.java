package com.jumbo.trus.service.notification.push.maker;

import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.football.helper.FootballMatchFormatter;
import com.jumbo.trus.service.notification.push.PushService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerNotificationMaker {

    private final PushService pushService;
    private final FootballMatchMapper footballMatchMapper;
    private final UserService userService;
    private final DeviceTokenRepository deviceTokenRepository;

    public void sendBeerNotify(BeerEntity beer, BeerDTO oldBeer) {
        List<DeviceToken> deviceTokenList = deviceTokenRepository.findDeviceTokensByPlayerId(beer.getPlayer().getId(), "ACTIVE");
        if (deviceTokenList.isEmpty()) {
            return;
        }
        BeerEntity diffBeer = calculateDiff(beer, oldBeer);
        if (diffBeer.getBeerNumber() == 0 && diffBeer.getLiquorNumber() == 0) {
            return;
        }

        for (DeviceToken deviceToken : deviceTokenList) {
            try {
                Map<String, String> data = getStringStringMap(diffBeer);

                pushService.sendPush(
                        deviceToken,
                        getTitle(diffBeer),
                        getBody(diffBeer),
                        NotificationType.BEER,
                        data
                );
            } catch (Exception e) {
                log.error("error:", e);
            }
        }
    }

    private static @NotNull Map<String, String> getStringStringMap(BeerEntity diffBeer) {
        Map<String, String> data = new java.util.HashMap<>();
        data.put("screenId", "beer-simple-screen");
        data.put("notificationType", NotificationType.BEER.name());
        data.put("navigateText", "Ukaž to!");

        if (diffBeer.getMatch() != null && diffBeer.getMatch().getId() != null) {
            data.put("matchId", diffBeer.getMatch().getId().toString());
        }

        if (diffBeer.getPlayer() != null) {
            data.put("playerId", String.valueOf(diffBeer.getPlayer().getId()));
        }
        return data;
    }

    private BeerEntity calculateDiff(BeerEntity beerEntity, BeerDTO oldBeer) {
        BeerEntity diffBeer = new BeerEntity();
        diffBeer.setMatch(beerEntity.getMatch());
        diffBeer.setPlayer(beerEntity.getPlayer());
        diffBeer.setAppTeam(beerEntity.getAppTeam());
        if (oldBeer == null) {
            diffBeer.setBeerNumber(beerEntity.getBeerNumber());
            diffBeer.setLiquorNumber(beerEntity.getLiquorNumber());
        } else {
            diffBeer.setBeerNumber(beerEntity.getBeerNumber() - oldBeer.getBeerNumber());
            diffBeer.setLiquorNumber(beerEntity.getLiquorNumber() - oldBeer.getLiquorNumber());
        }
        return diffBeer;
    }

    private String getTitle(BeerEntity beer) {
        if (beer.getBeerNumber() > 0 && beer.getLiquorNumber() == 0) {
            return "Právě vám bylo připsáno pivko!";
        } else if (beer.getBeerNumber() > 0 && beer.getLiquorNumber() > 0) {
            return "Právě vám bylo připsáno pivko a panák!";
        } else if (beer.getBeerNumber() > 0) {
            return "Právě vám bylo připsáno pivko a odebrán panák!";
        } else if (beer.getBeerNumber() < 0 && beer.getLiquorNumber() == 0) {
            return "Právě vám bylo odebráno pivko!";
        } else if (beer.getBeerNumber() < 0 && beer.getLiquorNumber() < 0) {
            return "Právě vám bylo odebráno pivko a panák!";
        } else if (beer.getBeerNumber() < 0) {
            return "Právě vám bylo odebráno pivko a přidán panák!";
        } else if (beer.getLiquorNumber() < 0) {
            return "Právě vám byl odebrán panák!";
        } else if (beer.getLiquorNumber() > 0) {
            return "Právě vám byl přidán panák!";
        } else {
            return "";
        }
    }


    private String getBody(BeerEntity beer) {
        String user = userService.getCurrentUser().getName();
        if (beer.getMatch().getFootballMatch() != null) {
            return "V zápase " + FootballMatchFormatter.toStringBasic(footballMatchMapper.toDTO(beer.getMatch().getFootballMatch())) + " uživatelem " + user;
        }
        return "Uživatelem " + user;
    }
}
