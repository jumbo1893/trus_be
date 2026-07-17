package com.jumbo.trus.entity.weather;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum WeatherCode {

    CLEAR_SKY(0, "Jasno"),

    MAINLY_CLEAR(1, "Převážně jasno"),
    PARTLY_CLOUDY(2, "Polojasno"),
    OVERCAST(3, "Zataženo"),

    FOG(45, "Mlha"),
    DEPOSITING_RIME_FOG(48, "Námrazová mlha"),

    LIGHT_DRIZZLE(51, "Slabé mrholení"),
    MODERATE_DRIZZLE(53, "Střední mrholení"),
    DENSE_DRIZZLE(55, "Husté mrholení"),

    LIGHT_FREEZING_DRIZZLE(56, "Slabé mrznoucí mrholení"),
    DENSE_FREEZING_DRIZZLE(57, "Husté mrznoucí mrholení"),

    SLIGHT_RAIN(61, "Slabý déšť"),
    MODERATE_RAIN(63, "Střední déšť"),
    HEAVY_RAIN(65, "Silný déšť"),

    LIGHT_FREEZING_RAIN(66, "Slabý mrznoucí déšť"),
    HEAVY_FREEZING_RAIN(67, "Silný mrznoucí déšť"),

    SLIGHT_SNOW_FALL(71, "Slabé sněžení"),
    MODERATE_SNOW_FALL(73, "Střední sněžení"),
    HEAVY_SNOW_FALL(75, "Silné sněžení"),

    SNOW_GRAINS(77, "Sněhová zrna"),

    SLIGHT_RAIN_SHOWERS(80, "Slabé dešťové přeháňky"),
    MODERATE_RAIN_SHOWERS(81, "Střední dešťové přeháňky"),
    VIOLENT_RAIN_SHOWERS(82, "Silné dešťové přeháňky"),

    SLIGHT_SNOW_SHOWERS(85, "Slabé sněhové přeháňky"),
    HEAVY_SNOW_SHOWERS(86, "Silné sněhové přeháňky"),

    THUNDERSTORM(95, "Bouřka"),
    THUNDERSTORM_WITH_SLIGHT_HAIL(
            96,
            "Bouřka se slabým krupobitím"
    ),
    THUNDERSTORM_WITH_HEAVY_HAIL(
            99,
            "Bouřka se silným krupobitím"
    );

    @Getter
    private final int code;
    private final String czechDescription;

    WeatherCode(int code, String czechDescription) {
        this.code = code;
        this.czechDescription = czechDescription;
    }

    /**
     * Jackson vrátí v JSON český text místo názvu enumu.
     *
     * PARTLY_CLOUDY -> "Polojasno"
     */
    @JsonValue
    public String getCzechDescription() {
        return czechDescription;
    }

    public static WeatherCode fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (WeatherCode weatherCode : values()) {
            if (weatherCode.code == code) {
                return weatherCode;
            }
        }

        return null;
    }

    /**
     * Použije se při případné deserializaci JSON.
     * Podporuje český text i název enumu.
     */
    @JsonCreator
    public static WeatherCode fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (WeatherCode weatherCode : values()) {
            if (weatherCode.czechDescription.equalsIgnoreCase(value)
                    || weatherCode.name().equalsIgnoreCase(value)) {
                return weatherCode;
            }
        }

        throw new IllegalArgumentException(
                "Neznámá hodnota WeatherCode: " + value
        );
    }

    public static String getDescription(Integer code) {
        WeatherCode weatherCode = fromCode(code);

        return weatherCode == null
                ? "Neznámé počasí"
                : weatherCode.getCzechDescription();
    }

    public static boolean isRain(Integer code) {
        return code != null && (
                code == 51
                        || code == 53
                        || code == 55
                        || code == 56
                        || code == 57
                        || code == 61
                        || code == 63
                        || code == 65
                        || code == 66
                        || code == 67
                        || code == 80
                        || code == 81
                        || code == 82
                        || code == 95
                        || code == 96
                        || code == 99
        );
    }

    public static boolean isSnow(Integer code) {
        return code != null && (
                code == 71
                        || code == 73
                        || code == 75
                        || code == 77
                        || code == 85
                        || code == 86
        );
    }

    public static boolean isStorm(Integer code) {
        return code != null && (
                code == 95
                        || code == 96
                        || code == 99
        );
    }

    public static boolean isMist(Integer code) {
        return code != null && (
                code == 45
                        || code == 48
        );
    }
}