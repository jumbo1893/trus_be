package com.jumbo.trus.dto.footbar.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootbarProfilePage {
    private Integer count;
    private String next;
    private String previous;
    private List<FootbarProfile> results;
}
