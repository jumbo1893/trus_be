package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.strava.AthleteActivities;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.activity.StravaService;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/strava")
@RequiredArgsConstructor
public class StravaController {

    private final StravaService stravaService;
    private final AppTeamService appTeamService;

    @RoleRequired("READER")
    @GetMapping("/connect")
    public Map<String, String> connectToStrava() {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String redirectUrl = stravaService.connectRedirectUrl(user.getId());
        return Map.of("url", redirectUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code, @RequestParam String state) {
        try {
            Long userId = Long.parseLong(state);
            stravaService.exchangeCodeForToken(code, userId);
            return ResponseEntity.ok("""
                <html>
                  <body>
                    <h2>Připojení ke Stravě bylo úspěšné.</h2>
                    <p>Můžeš zavřít tuto stránku a vrátit se zpět do mobilní aplikace.</p>
                  </body>
                </html>
            """);
        } catch (Exception e) {
            log.debug(String.valueOf(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<h2>Nastala chyba při propojení se Stravou.</h2>");
        }
    }

    @RoleRequired("ADMIN")
    @PostMapping("/sync/{athleteId}")
    public ResponseEntity<String> syncActivitiesForAthlete(@PathVariable Long athleteId) {
        stravaService.syncActivities(athleteId);
        return ResponseEntity.ok("Activities synced successfully.");
    }

    @RoleRequired("ADMIN")
    @PostMapping("/sync")
    public void syncActivities() {
        stravaService.syncActivities(appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-football-match")
    public List<AthleteActivities> getFootballMatchActivities(@RequestParam Long footballMatchId) {
        return stravaService.getListOfAthletesByFootballMatch(appTeamService.getCurrentAppTeamOrThrow(), footballMatchId);
    }
}
