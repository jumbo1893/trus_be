package com.jumbo.trus.service.achievement.rule;

import com.jumbo.trus.dto.VisitedCountryResponse;

import java.util.function.Function;
import java.util.function.Predicate;

public record CountryAchievementRule(
        String achievementCode,
        Predicate<VisitedCountryResponse> condition,
        Function<VisitedCountryResponse, String> detailFactory
) {

    public boolean isSatisfiedBy(VisitedCountryResponse country) {
        return condition.test(country);
    }

    public String createDetail(VisitedCountryResponse country) {
        return detailFactory.apply(country);
    }
}