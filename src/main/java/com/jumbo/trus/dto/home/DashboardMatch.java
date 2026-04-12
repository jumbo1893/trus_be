package com.jumbo.trus.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.helper.TextWithRedirect;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMatch {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FootballMatchDetail match;

    private List<TextWithRedirect> matchInfoList;

}
