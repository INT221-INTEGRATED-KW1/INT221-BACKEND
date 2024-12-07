package sit.int221.integratedproject.kanbanborad.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.response.AuthenticateUser;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.enumeration.RoleEnum;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.UserOwnRepository;


@Service
public class MicrosoftUserDetailsService {
    private final UserOwnRepository userOwnRepository;

    public MicrosoftUserDetailsService(UserOwnRepository userOwnRepository) {
        this.userOwnRepository = userOwnRepository;
    }

    public UserDetails loadUserByOid(String oid) throws UsernameNotFoundException {
        UserOwn userOwn = userOwnRepository.findById(oid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Microsoft user with OID " + oid + " does not exist !!"));
        return getUserDetails(userOwn);
    }

    private UserDetails getUserDetails(UserOwn userOwn) {
        RoleEnum role = RoleEnum.STUDENT;
        return new AuthenticateUser(userOwn.getOid(), userOwn.getUsername(), null, role);
    }
}
