package com.jumbo.trus.dto.helper;

import com.jumbo.trus.dto.SeasonDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedirectDTO {

    private Redirect redirect;
    private SeasonDTO season;
}
