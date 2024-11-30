package com.jumbo.trus.service.football.pkfl.task;

import lombok.NoArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;

@NoArgsConstructor
public class LoginToPkfl {

    @Value("${pkfl.login_page}")
    private static String PKFL_LOGIN_PAGE;

    @Value("${pkfl.login_mail}")
    private static String PKFL_LOGIN_MAIL;

    @Value("${pkfl.login_password}")
    private static String PKFL_LOGIN_PASSWORD;

    public Connection.Response getLoggedAccessToPkflWeb(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url).method(Connection.Method.GET).execute();
        Elements logout = res.parse().getElementsByClass("fa fa-sign-out");
        if (!logout.isEmpty()) {
            return res;
        }
        return Jsoup.connect(url).method(Connection.Method.GET).cookies(loginToPkflWeb()).execute();

    }

    private Map<String, String> loginToPkflWeb() throws IOException {
        Connection.Response res = Jsoup.connect(PKFL_LOGIN_PAGE)
                .data("email", PKFL_LOGIN_MAIL)
                .data("password", PKFL_LOGIN_PASSWORD)
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
