package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Attachment;

import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {
    Optional<Attachment> findByTaskIdAndFilename(Integer taskId, String fileName);
}
