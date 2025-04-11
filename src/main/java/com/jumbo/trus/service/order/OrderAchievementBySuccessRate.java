package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.achievement.AchievementDetail;

import java.util.Comparator;

public class OrderAchievementBySuccessRate implements Comparator<AchievementDetail> {

    public int compare(AchievementDetail o1, AchievementDetail o2) {
        return o2.getSuccessRate().compareTo(o1.getSuccessRate());
    }
}
