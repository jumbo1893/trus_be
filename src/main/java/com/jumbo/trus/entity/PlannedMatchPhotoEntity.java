package com.jumbo.trus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "planned_match_photo")
@Getter
@Setter
public class PlannedMatchPhotoEntity {
    @Id
    private Long plannedMatchId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] image;
}
