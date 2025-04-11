package com.jumbo.trus.dto.football;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootballPlayerDTO {

    private Long id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<@Positive Long> teamIdList;

    private String name;

    private int birthYear;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phoneNumber;

    private String uri;

    public FootballPlayerDTO(String name, int birthYear, String email, String phoneNumber, String uri) {
        this.name = name;
        this.birthYear = birthYear;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.uri = uri;
    }

    public FootballPlayerDTO(@NotNull List<@Positive Long> teamIdList, String name, int birthYear, String email, String phoneNumber, String uri) {
        this.teamIdList = teamIdList;
        this.name = name;
        this.birthYear = birthYear;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FootballPlayerDTO that = (FootballPlayerDTO) o;
        return birthYear == that.birthYear && Objects.equals(name, that.name) && Objects.equals(email, that.email) && Objects.equals(phoneNumber, that.phoneNumber) && Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, birthYear, email, phoneNumber, uri);
    }
}
