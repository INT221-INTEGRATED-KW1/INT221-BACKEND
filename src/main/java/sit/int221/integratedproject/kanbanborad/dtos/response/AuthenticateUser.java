package sit.int221.integratedproject.kanbanborad.dtos.response;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import sit.int221.integratedproject.kanbanborad.enumeration.RoleEnum;

import java.util.Collection;
import java.util.List;

public record AuthenticateUser(
        String oid,
        String username,
        String password,
        RoleEnum role) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return switch(this.role) {
            case LECTURER -> List.of(new SimpleGrantedAuthority(RoleEnum.LECTURER.name()));
            case STAFF -> List.of(new SimpleGrantedAuthority(RoleEnum.STAFF.name()));
            default -> List.of(new SimpleGrantedAuthority(RoleEnum.STUDENT.name()));
        };
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public String getOid() {
        return this.oid;
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