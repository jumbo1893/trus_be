package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.footbar.profile.FootbarProfile;
import com.jumbo.trus.dto.footbar.response.FootbarSessionSetup;
import com.jumbo.trus.service.activity.footbar.FootbarService;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/footbar")
@RequiredArgsConstructor
public class FootbarController {

    private final AppTeamService appTeamService;
    private final FootbarService footbarService;
    private final UserService userService;

    @RoleRequired("READER")
    @GetMapping("/connect")
    public Map<String, String> connectToFootbar() {
        String redirectUrl = footbarService.connectRedirectUrl();
        return Map.of("url", redirectUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback() {
        return ResponseEntity.ok("""
                <html>
                  <body>
                    <h2>Připojení k Footbar bude pokračovat v aplikaci.</h2>
                    <p>Můžeš zavřít tuto stránku a vrátit se zpět do mobilní aplikace.</p>
                  </body>
                </html>
            """);
    }

    @RoleRequired("READER")
    @PostMapping("/connect/exchange")
    public ResponseEntity<Map<String, Boolean>> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        try {
            footbarService.exchangeCodeForToken(code);
            return ResponseEntity.ok(Map.of("connected", true));
        } catch (Exception e) {
            log.error("Neočekávaná chyba REST volání: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("connected", false));
        }
    }

    @RoleRequired("ADMIN")
    @PostMapping("/sync/{athleteId}")
    public ResponseEntity<String> syncActivitiesForAthlete(@PathVariable Long accountId) {
        footbarService.syncSession(accountId, appTeamService.getCurrentAppTeamOrThrow());
        return ResponseEntity.ok("Sessions synced successfully.");
    }

    @RoleRequired("ADMIN")
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Date>> syncActivities() {
        try {
            Date date = footbarService.syncSessions(appTeamService.getCurrentAppTeamOrThrow());
            return ResponseEntity.ok(Map.of("date", date));
        } catch (Exception e) {
            log.error("Neočekávaná chyba REST volání: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("date", new Date()));
        }
    }

    @RoleRequired("READER")
    @GetMapping("/sessions/setup")
    public FootbarSessionSetup getFootballMatchActivities(@RequestParam(required = false) Long seasonId) {
        return footbarService.getFootbarSessionCompareSetup(seasonId, appTeamService.getCurrentAppTeamOrThrow(), userService.getCurrentUserEntity().getId());
    }

    @RoleRequired("READER")
    @GetMapping("/profile")
    public FootbarProfile getCurrentFootbarProfile() {
        return footbarService.getFootbalProfile(userService.getCurrentUserEntity().getId());
    }

    @RoleRequired("READER")
    @GetMapping("/sync/date")
    public ResponseEntity<Map<String, Date>> getSessionLastSyncDate() {
        Date date = footbarService.getLastSyncDate(appTeamService.getCurrentAppTeamOrThrow());
        Map<String, Date> body = new HashMap<>();
        body.put("date", date);
        return ResponseEntity.ok(body);
    }
}
