package com.jumbo.trus.repository;

import com.jumbo.trus.entity.OAuthStateEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthStateRepository extends JpaRepository<OAuthStateEntity, Long> {

    @Query("""
            SELECT o
            FROM OAuthStateEntity o
            WHERE o.user = :user
              AND o.system = :system
              AND o.expiresIn > CURRENT_TIMESTAMP
            """)
    Optional<OAuthStateEntity> findValidByUserAndSystem(@Param("user") UserEntity user,
                                                        @Param("system") String system);

    void deleteByUserAndSystem(UserEntity userEntity, String system);

}

