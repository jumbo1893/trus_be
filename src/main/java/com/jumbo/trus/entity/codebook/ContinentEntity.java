package com.jumbo.trus.entity.codebook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "continent", schema = "codebook")
public class ContinentEntity {

    @Id
    @Column(name = "code", length = 2, nullable = false, updatable = false)
    private String code;

    @Column(name = "name_cs", nullable = false, unique = true, length = 50)
    private String nameCs;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}