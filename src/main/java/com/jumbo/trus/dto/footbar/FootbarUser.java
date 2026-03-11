package com.jumbo.trus.dto.footbar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootbarUser {
    private Long id;
    private String username;
    private String firstname;
    private String lastname;
}