package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.service.football.pkfl.PkflProperties;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoginToPkfl {

    private final PkflProperties pkflProperties;

    /*@Value("${pkfl.login_page}")
    private static String PKFL_LOGIN_PAGE;

    @Value("${pkfl.login_mail}")
    private static String PKFL_LOGIN_MAIL;

    @Value("${pkfl.login_password}")
    private static String PKFL_LOGIN_PASSWORD;*/

    public Connection.Response getLoggedAccessToPkflWeb(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url).method(Connection.Method.GET).execute();
        Elements logout = res.parse().getElementsByClass("fa fa-sign-out");
        if (!logout.isEmpty()) {
            return res;
        }
        return Jsoup.connect(url).method(Connection.Method.GET).cookies(loginToPkflWeb()).execute();

    }

    private Map<String, String> loginToPkflWeb() throws IOException {
        Connection.Response res = Jsoup.connect(pkflProperties.getLoginPage())
                .data("email", pkflProperties.getLoginMail())
                .data("password", pkflProperties.getLoginPassword())
                .data("send", "Přihlásit")
                .data("_do", "signInForm-submit")
                .method(Connection.Method.POST)
                .execute();
        if (res.statusCode() != 200 || res.parse().title().equals("PKFL | Přihlášení")) {
            throw new IOException("Nelze se přihlásit");
        }
        return res.cookies();
    }
}
