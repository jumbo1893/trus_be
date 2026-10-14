package com.jumbo.trus.dto.football.detail;

/** An estimate based only on whether the given name occurs in the Czech name-day calendar. */
public record ForeignNameEstimate(Integer rosterPercent, Integer lastMatchPercent) {
}
