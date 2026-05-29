package com.jumbo.trus.dto.receivedfine.response.stats.match;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class StatsMatchDTO {

    private Long id;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;

    private Long seasonId;
}