package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.match.MatchDTO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatter {

    public static String formatDateForFrontend(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        return dateFormat.format(date);
    }

    public static String formatToMatchDate(MatchDTO matchDTO) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));
        return dateFormat.format(matchDTO.getDate());
    }
}
