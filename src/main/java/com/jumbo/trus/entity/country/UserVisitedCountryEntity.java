package com.jumbo.trus.entity.country;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.codebook.CountryEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(
        name = "user_visited_country",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_visited_country",
                        columnNames = {"user_id", "country_code"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_visited_country_user",
                        columnList = "user_id"
                )
        }
)
public class UserVisitedCountryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_visited_country_user")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "country_code",
            nullable = false,
            referencedColumnName = "code",
            foreignKey = @ForeignKey(name = "fk_user_visited_country_country")
    )
    private CountryEntity country;

    @Column(name = "first_visited_at", nullable = false, updatable = false)
    private LocalDateTime firstVisitedAt;
}