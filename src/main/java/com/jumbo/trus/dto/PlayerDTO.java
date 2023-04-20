package com.jumbo.trus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jumbo.trus.dto.beer.BeerDTO;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {

    @JsonProperty("_id")
    private long id;

    private String name;

    private Date birthday;

    private boolean fan;

    private boolean active;
}
