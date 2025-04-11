package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.player.PlayerDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BirthdayCalculator {
    final List<PlayerDTO> players;
    Calendar currentDate = Calendar.getInstance();

    public BirthdayCalculator(List<PlayerDTO> players) {
        if (players.isEmpty()) {
            this.players = players;
        }
        else {
            this.players = filterPlayersByUpcomingBirthday(players);
        }
        currentDate.setTime(new Date());
    }

    private List<PlayerDTO> filterPlayersByUpcomingBirthday(List<PlayerDTO> incomingPlayers) {
        List<PlayerDTO> returnPlayers = new ArrayList<>();
        Calendar playerCalendar = Calendar.getInstance();
        playerCalendar.setTime(incomingPlayers.get(0).getBirthday());
        for (PlayerDTO playerDTO : incomingPlayers) {
            Calendar incomingCalendar = Calendar.getInstance();
            incomingCalendar.setTime(playerDTO.getBirthday());
            if (incomingCalendar.get(Calendar.MONTH) == playerCalendar.get(Calendar.MONTH) && incomingCalendar.get(Calendar.DAY_OF_MONTH) == playerCalendar.get(Calendar.DAY_OF_MONTH)) {
                returnPlayers.add(playerDTO);
            }
        }
        return returnPlayers;
    }


    public String returnNextPlayerBirthdayFromList() {
        if (players.isEmpty()) {
            return "Nelze najít dny do narozenin hráčů, hrajou vůbec nějaký za Trus?";
        } else if (players.size() == 1) {
            PlayerDTO player = players.get(0);
            if (isTodayBirthday(player)) {
                return ("Dnes slaví narozeniny " + (player.isFan() ? "fanoušek " : "hráč ") + player.getName() + ", který má " + calculateAge(player) + " let. Už ten sud vyval a ať ti slouží splávek!");
            } else {
                return ("Příští rundu platí " + (player.isFan() ? "věrný fanoušek " : "") + player.getName() + ", který bude mít za " + timeToNextBirthdayToString(player) + " své " + calculateAge(player) + ". narozeniny");
            }
        } else {
            PlayerDTO player = players.get(0);
            if (isTodayBirthday(player)) {
                StringBuilder text = new StringBuilder("Dnešní oslavenci jsou ");
                for (int i = 0; i < players.size(); i++) {
                    if (i == players.size() - 1) {
                        text.append(players.get(i).getName()).append(" ");
                    } else if (i == players.size() - 2) {
                        text.append(players.get(i).getName()).append(" a ");
                    } else {
                        text.append(players.get(i).getName()).append(", ");
                    }
                }
                text.append("kteří slaví své nádherné ");
                for (int i = 0; i < players.size(); i++) {
                    text.append(calculateAge(players.get(i))).append(".");
                    if (i == players.size() - 1) {
                        text.append(" narozeniny. Pánové, všichni doufáme, že se pochlapíte. Na Trus!!!!");
                    } else if (i == players.size() - 2) {
                        text.append(" a ");
                    } else {
                        text.append(", ");
                    }
                }
                return text.toString();
            } else {
                StringBuilder text = new StringBuilder("Příští rundu platí ");
                for (int i = 0; i < players.size(); i++) {
                    if (i == players.size() - 1) {
                        text.append(players.get(i).getName()).append(" ");
                    } else if (i == players.size() - 2) {
                        text.append(players.get(i).getName()).append(" a ");
                    } else {
                        text.append(players.get(i).getName()).append(", ");
                    }
                }
                text.append("kteří mají za ").append(timeToNextBirthdayToString(player)).append(" své ");
                for (int i = 0; i < players.size(); i++) {
                    text.append(calculateAge(players.get(i))).append(".");
                    if (i == players.size() - 1) {
                        text.append(" narozeniny");
                    } else if (i == players.size() - 2) {
                        text.append(" a ");
                    } else {
                        text.append(", ");
                    }
                }
                return text.toString();
            }
        }
    }


    private boolean isTodayBirthday(PlayerDTO playerDTO) {
        Calendar playerCalendar = Calendar.getInstance();
        playerCalendar.setTime(playerDTO.getBirthday());
        return currentDate.get(Calendar.DAY_OF_MONTH) == playerCalendar.get(Calendar.DAY_OF_MONTH)
                && currentDate.get(Calendar.MONTH) == playerCalendar.get(Calendar.MONTH);
    }

    private int calculateAge(PlayerDTO playerDTO) {
        Calendar playerCalendar = Calendar.getInstance();
        playerCalendar.setTime(playerDTO.getBirthday());
        int age = currentDate.get(Calendar.YEAR) - playerCalendar.get(Calendar.YEAR);
        /*if (isTodayBirthday(playerDTO)) {
            age--; // Ještě nemáte narozeniny v aktuálním roce
        }*/
        return age;
    }

    private boolean isBirthdayInThisYear(Calendar playerCalendar) {
        return currentDate.get(Calendar.MONTH) < playerCalendar.get(Calendar.MONTH) ||
                (currentDate.get(Calendar.MONTH) == playerCalendar.get(Calendar.MONTH) &&
                        currentDate.get(Calendar.DAY_OF_MONTH) <= playerCalendar.get(Calendar.DAY_OF_MONTH));
    }


    private Period periodToBirthday(PlayerDTO playerDTO) {
        Calendar playerCalendar = Calendar.getInstance();
        playerCalendar.setTime(playerDTO.getBirthday());
        if (isBirthdayInThisYear(playerCalendar)) {
            playerCalendar.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
        } else {
            playerCalendar.set(Calendar.YEAR, currentDate.get(Calendar.YEAR) + 1);
        }
        Instant playerInstant = playerCalendar.toInstant();
        Instant currentInstant = new Date().toInstant();
        LocalDate playerDate = playerInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate currentDate = currentInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(currentDate, playerDate);
    }


    private String timeToNextBirthdayToString(PlayerDTO playerDTO) {
        String monthString = monthsToNextBirtdayToString(playerDTO);
        String dayString = daysToNextBirtdayToString(playerDTO);
        if (!monthString.isEmpty() && !dayString.isEmpty()) {
            return monthString + " a " + dayString;
        }
        else if (!monthString.isEmpty()) {
            return monthString;
        }
        else {
            return dayString;
        }
    }

    private String daysToNextBirtdayToString(PlayerDTO playerDTO) {
        Period periodToBirthday = periodToBirthday(playerDTO);
        String text = "";
        if (periodToBirthday.getDays() == 1) {
            text += periodToBirthday.getDays() + " den";
        } else if (periodToBirthday.getDays() == 2 || periodToBirthday.getDays() == 3 || periodToBirthday.getDays() == 4) {
            text += periodToBirthday.getDays() + " dny";
        } else if (periodToBirthday.getDays() == 0) {
            text += "";
        } else {
            text += periodToBirthday.getDays() + " dní";
        }
        return text;
    }

    private String monthsToNextBirtdayToString(PlayerDTO playerDTO) {
        Period periodToBirthday = periodToBirthday(playerDTO);
        String text = "";
        if (periodToBirthday.getMonths() == 1) {
            text += periodToBirthday.getMonths() + " měsíc";
        } else if (periodToBirthday.getMonths() == 2 || periodToBirthday.getDays() == 3 || periodToBirthday.getDays() == 4) {
            text += periodToBirthday.getMonths() + " měsíce";
        } else if (periodToBirthday.getMonths() == 0) {
            text += "";
        } else {
            text += periodToBirthday.getMonths() + " měsíců";
        }
        return text;
    }

}
