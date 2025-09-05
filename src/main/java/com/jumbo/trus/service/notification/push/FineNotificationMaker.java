package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.football.helper.FootballMatchFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FineNotificationMaker {

    private final PushService pushService;
    private final FootballMatchMapper footballMatchMapper;
    private final UserService userService;
    private final DeviceTokenRepository deviceTokenRepository;

    public void sendFineNotify(ReceivedFineEntity fine, ReceivedFineDTO oldFine) {
        List<DeviceToken> deviceTokenList = deviceTokenRepository.findDeviceTokensByPlayerId(fine.getPlayer().getId());
        if (deviceTokenList.isEmpty()) {
            return;
        }
        ReceivedFineEntity diffFine = calculateDiff(fine, oldFine);
        if (diffFine.getFineNumber() == 0) {
            return;
        }

        for (DeviceToken deviceToken : deviceTokenList) {
            try {
                pushService.sendPush(deviceToken, getTitle(diffFine), getBody(diffFine));
            } catch (Exception e) {
                log.error("error:", e);
            }
        }
    }

    private ReceivedFineEntity calculateDiff(ReceivedFineEntity receivedFine, ReceivedFineDTO oldFine) {
        ReceivedFineEntity diffFine = new ReceivedFineEntity();
        diffFine.setMatch(receivedFine.getMatch());
        diffFine.setPlayer(receivedFine.getPlayer());
        diffFine.setAppTeam(receivedFine.getAppTeam());
        diffFine.setFine(receivedFine.getFine());
        if (oldFine == null) {
            diffFine.setFineNumber(receivedFine.getFineNumber());
        } else {
            diffFine.setFineNumber(receivedFine.getFineNumber() - oldFine.getFineNumber());
        }
        return diffFine;
    }

    private String getTitle(ReceivedFineEntity fine) {
        if (fine.getFineNumber() > 0) {
            return "Právě vám přidaná pokuta!";
        } else if (fine.getFineNumber() < 0) {
            return "Právě vám odebraná pokuta";
        } else {
            return "";
        }
    }


    private String getBody(ReceivedFineEntity fine) {
        String user = userService.getCurrentUser().getName();
        StringBuilder body = new StringBuilder();
        body.append(Math.abs(fine.getFineNumber())).append("x ").append(fine.getFine().getName()).append(" v hodnotě ").append(fine.getFine().getAmount()).append(" Kč");
        if (fine.getMatch().getFootballMatch() != null) {
            body.append(" v zápase ").append(FootballMatchFormatter.toStringBasic(footballMatchMapper.toDTO(fine.getMatch().getFootballMatch()))).append(" uživatelem ").append(user);
        }
        else {
            body.append(" uživatelem ").append(user);
        }
        return body.toString();
    }
}
