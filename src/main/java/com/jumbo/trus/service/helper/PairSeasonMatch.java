package com.jumbo.trus.service.helper;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PairSeasonMatch {

    private SeasonDTO seasonDTO;
    private MatchDTO matchDTO;
}
