package com.jumbo.trus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FineDTO {

    private long id;

    private String name;

    private int amount;

    private boolean inactive;

}
