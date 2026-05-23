package com.jumbo.trus.dto.achievement;

public interface IPlayerAchievementStats {

    Long getPlayerId();

    Long getAccomplishedCount();

    Long getNotAccomplishedCount();
}