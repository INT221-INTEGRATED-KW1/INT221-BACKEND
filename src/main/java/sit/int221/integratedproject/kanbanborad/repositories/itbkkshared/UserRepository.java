package sit.int221.integratedproject.kanbanborad.repositories.itbkkshared;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByOid(String oid);
}
