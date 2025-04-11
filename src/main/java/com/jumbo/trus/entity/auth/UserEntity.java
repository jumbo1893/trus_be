package com.jumbo.trus.entity.auth;

import com.jumbo.trus.entity.football.TableTeamEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity(name = "auth")
@Data
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mail;


    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean admin = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTeamRole> teamRoles;

    @OneToMany(mappedBy = "owner")
    private List<AppTeamEntity> appTeamsOwner;

    @Override
    public String getUsername() {
        return mail;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (UserTeamRole teamRole : teamRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + teamRole.getRole().toUpperCase() + "_TEAM_" + teamRole.getAppTeam().getId()));
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
