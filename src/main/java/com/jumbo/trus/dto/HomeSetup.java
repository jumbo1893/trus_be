package com.jumbo.trus.dto;

import com.jumbo.trus.dto.match.MatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeSetup {

    @NotNull
    private String nextBirthday;

    private List<String> randomFacts;

}
