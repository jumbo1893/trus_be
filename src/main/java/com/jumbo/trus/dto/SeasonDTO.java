package com.jumbo.trus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDTO {

    @JsonProperty("_id")
    private long id;

    private String name;

    private Date fromDate;
    
    private Date toDate;

    //private List<MatchDTO> matchList;
}
