package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.RefreshToken;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;

import java.sql.Ref;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    RefreshToken findRefreshTokenByToken(String token);
}
