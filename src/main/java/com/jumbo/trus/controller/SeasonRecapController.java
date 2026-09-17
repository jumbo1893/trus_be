package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.service.auth.*;
import com.jumbo.trus.service.recap.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping(value="/season-recap", produces="application/json") @RequiredArgsConstructor
public class SeasonRecapController {
    private final SeasonRecapService service;
    private final UserService users;
    private final AppTeamService teams;
    @GetMapping @RoleRequired("READER")
    public List<SeasonRecapService.Summary> list(){return service.list(users.getCurrentUserEntity().getId(),teams.getCurrentAppTeamOrThrow().getId());}
    @GetMapping("/{id}") @RoleRequired("NONE")
    public SeasonRecap detail(@PathVariable long id){return service.detail(id,users.getCurrentUserEntity().getId());}
    @PostMapping("/{id}/opened") @RoleRequired("NONE")
    public java.util.Map<String,Boolean> opened(@PathVariable long id){
        service.opened(id,users.getCurrentUserEntity().getId());
        return java.util.Map.of("opened",true);
    }
    @PostMapping("/{id}/regenerate") @RoleRequired("NONE")
    public SeasonRecapService.Summary regenerate(@PathVariable long id,
            @RequestParam(defaultValue="false") boolean resetOpened) {
        return service.regenerate(id,users.getCurrentUserEntity().getId(),resetOpened);
    }
}
