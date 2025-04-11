package com.jumbo.trus.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;

import java.util.Date;

@Entity(name = "step_update")
@Data
public class StepUpdateEntity {

    @Id
    @GeneratedValue(generator = "step_update_seq")
    @SequenceGenerator(name = "step_update_seq", sequenceName = "step_update_seq", allocationSize = 1)
    private Long id;

    private Long userId;

    private int stepNumber;

    private Date updateTime;

}
