package com.jumbo.trus.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

import java.util.Date;

@Entity(name = "update")
@Data
public class UpdateEntity {

    @Id
    @GeneratedValue(generator = "update_seq")
    @SequenceGenerator(name = "update_seq", sequenceName = "update_seq", allocationSize = 1)
    private Long id;

    private String name;

    private Date date;

}
