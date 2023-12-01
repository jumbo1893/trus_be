package com.jumbo.trus.dto.pkfl.stats;

import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PkflCardComment {

    @NotNull
    private PkflMatchDTO pkflMatch;

    String comment;

}
