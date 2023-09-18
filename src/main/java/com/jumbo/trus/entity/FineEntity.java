package com.jumbo.trus.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;
import java.util.List;

@Entity(name = "fine")
@Data
public class FineEntity {

    @Id
    @GeneratedValue(generator="fine_seq")
    @SequenceGenerator(name = "fine_seq", sequenceName = "fine_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int amount;

    @ColumnDefault("true")
    private boolean editable = true;

    @ColumnDefault("false")
    private boolean inactive = false;

    @OneToMany(mappedBy = "fine")
    private List<ReceivedFineEntity> receivedFineList;
}
