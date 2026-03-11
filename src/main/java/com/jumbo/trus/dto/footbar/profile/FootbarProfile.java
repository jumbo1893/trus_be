package com.jumbo.trus.dto.footbar.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class FootbarProfile {

    @JsonAlias("user_id")
    private Long userId;

    private String nickname;

    @JsonAlias("fav_foot")
    private String favFoot;

    @JsonAlias("fav_position")
    private String favPosition;

    @JsonAlias("first_name")
    private String firstName;

    @JsonAlias("last_name")
    private String lastName;

    private String gender;

    @JsonAlias("d_o_b")
    private String dateOfBirth;

    @JsonAlias("profile_pic")
    private String profilePic;

    @JsonAlias("age_category")
    private String ageCategory;

    private Float height;

    private Float weight;

    private String strength;

    @JsonAlias("country_flag")
    private String countryFlag;

    private Boolean active;
}

