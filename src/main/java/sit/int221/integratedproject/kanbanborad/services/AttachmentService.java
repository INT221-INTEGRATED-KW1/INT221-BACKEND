package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Attachment;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.AttachmentRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.TaskRepository;

import java.util.Optional;

@Service
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final FileService fileService;

    public AttachmentService(AttachmentRepository attachmentRepository, TaskRepository taskRepository, FileService fileService) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.fileService = fileService;
    }

    @Transactional
    public void deleteAttachmentAndFile(String id, Integer taskId, String fileName) {
        String dbFileName = id + "_" + taskId + "_" + fileName;

        fileService.deleteFile(id, taskId, fileName);

        Optional<Attachment> attachment = attachmentRepository.findByTaskIdAndFilename(taskId, dbFileName);
        if (attachment.isPresent()) {
            attachmentRepository.delete(attachment.get());

            int updatedAttachmentCount = attachmentRepository.countByTaskId(taskId);
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ItemNotFoundException("Task not found for id: " + taskId));
            task.setAttachmentCount(updatedAttachmentCount);
            taskRepository.save(task);
        } else {
            throw new ItemNotFoundException("Attachment not found for file: " + dbFileName);
        }
    }
}
