package com.jumbo.trus.dto.footbar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jumbo.trus.dto.footbar.api.FootbarSessionRawDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionListResponse {
    private Integer count;
    private String next;
    private String previous;
    private List<FootbarSessionRawDTO> results;
}