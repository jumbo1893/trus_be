package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.match.MatchDTO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatter {

    private static final TimeZone PRAGUE_TIME_ZONE = TimeZone.getTimeZone("Europe/Prague");

    public static String formatDateForFrontend(Date date) {
        if (date == null) {
            return "";
        }

        DateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy HH:mm");
        dateFormat.setTimeZone(PRAGUE_TIME_ZONE);
        return dateFormat.format(date);
    }

    public static String formatToMatchDate(MatchDTO matchDTO) {
        if (matchDTO == null) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy");
        dateFormat.setTimeZone(PRAGUE_TIME_ZONE);
        return dateFormat.format(matchDTO.getDate());
    }
}