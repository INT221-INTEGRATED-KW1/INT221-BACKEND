package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;

import java.util.Optional;

public interface UserOwnRepository extends JpaRepository<UserOwn, String> {
    Optional<UserOwn> findByOid(String oid);
    Optional<UserOwn> findByEmail(String email);
}
