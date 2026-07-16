package com.jumbo.trus.entity.codebook;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "country", schema = "codebook")
public class CountryEntity {

    @Id
    @Column(name = "code", length = 2, nullable = false, updatable = false)
    private String code;

    @Column(name = "name_cs", nullable = false, unique = true, length = 100)
    private String nameCs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "continent_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_country_continent")
    )
    private ContinentEntity continent;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}