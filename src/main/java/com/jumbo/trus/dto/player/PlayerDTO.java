package com.jumbo.trus.dto.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {

    private long id;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date birthday;

    private boolean fan;

    private boolean active;

    private FootballPlayerDTO footballPlayer;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerDTO playerDTO = (PlayerDTO) o;

        return id == playerDTO.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
