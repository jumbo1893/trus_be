package com.jumbo.trus.service.football.pkfl;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pkfl")
@Data
public class PkflProperties {

    private String loginPage;
    private String loginMail;
    private String loginPassword;
    private String trus;
    private String table;
}
