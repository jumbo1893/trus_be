package com.jumbo.trus.service.football.pkfl.task;

import com.jumbo.trus.service.football.pkfl.PkflProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoginToPkfl {

    private final PkflProperties pkflProperties;
    private Map<String, String> sessionCookies = Map.of();

    /*@Value("${pkfl.login_page}")
    private static String PKFL_LOGIN_PAGE;

    @Value("${pkfl.login_mail}")
    private static String PKFL_LOGIN_MAIL;

    @Value("${pkfl.login_password}")
    private static String PKFL_LOGIN_PASSWORD;*/

    public synchronized Document getLoggedDocument(String url) throws IOException {
        if (!sessionCookies.isEmpty()) {
            Document document = getDocument(url, sessionCookies);
            if (isLoggedIn(document)) {
                return document;
            }
        }

        sessionCookies = Map.copyOf(loginToPkflWeb());
        return getDocument(url, sessionCookies);
    }

    private Document getDocument(String url, Map<String, String> cookies) throws IOException {
        return Jsoup.connect(url)
                .method(Connection.Method.GET)
                .cookies(cookies)
                .execute()
                .parse();
    }

    private boolean isLoggedIn(Document document) {
        return document.selectFirst(".fa.fa-sign-out") != null;
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
