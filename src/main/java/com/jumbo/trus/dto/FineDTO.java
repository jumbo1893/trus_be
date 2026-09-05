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

    private String code;

    private boolean editable;

    /**
     * Kept for source and wire compatibility with the original four-field DTO.
     */
    public FineDTO(long id, String name, int amount, boolean inactive) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.inactive = inactive;
    }

}
