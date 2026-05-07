package com.jumbo.trus.dto.home.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsBoardData {

    private String title;

    private List<String> headers;

    private List<StatsBoardRow> rows;

}
