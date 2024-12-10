package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskUpdateRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseWithAttachmentsDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.services.*;

import java.util.List;

import static sit.int221.integratedproject.kanbanborad.utils.Utils.isMicrosoftToken;

@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = {"http://ip23kw1.sit.kmutt.ac.th", "http://intproj23.sit.kmutt.ac.th",
        "https://ip23kw1.sit.kmutt.ac.th", "https://intproj23.sit.kmutt.ac.th"})
public class TaskController {
    private final TaskService taskService;
    private final FileService fileService;
    private final AttachmentService attachmentService;

    public TaskController(TaskService taskService, FileService fileService, AttachmentService attachmentService) {
        this.taskService = taskService;
        this.fileService = fileService;
        this.attachmentService = attachmentService;
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask(@RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String[] filterStatuses,
                                                            @PathVariable String id,
                                                            @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = taskService.getBoardOrThrow(id);

        List<TaskResponseDTO> tasks = fetchTasksBasedOnParams(sortBy, filterStatuses, id);

        if (taskService.isPublicBoard(board)) {
            return ResponseEntity.ok(tasks);
        }
        Claims claims;

        if (isMicrosoftToken(token)) {
            claims = taskService.validateMicrosoftTokenAndAuthorization(token, id);
        } else {
            claims = taskService.validateToken(token);
        }

        taskService.validateOwnership(claims, id);

        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    private List<TaskResponseDTO> fetchTasksBasedOnParams(String sortBy, String[] filterStatuses, String boardId) {
        if (sortBy == null && filterStatuses == null) {
            return taskService.findAllTask(boardId);
        } else if (sortBy != null && filterStatuses == null) {
            return taskService.findAllTaskSorted(sortBy, boardId);
        } else if (sortBy == null && filterStatuses != null) {
            return taskService.findAllTaskFiltered(filterStatuses, boardId);
        } else {
            return taskService.findAllTaskSortedAndFiltered(sortBy, filterStatuses, boardId);
        }
    }

    @GetMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskDetailResponseDTO> getTaskById(@PathVariable String id,
                                                             @PathVariable Integer taskId,
                                                             @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = taskService.getBoardOrThrow(id);

        if (taskService.isPublicBoard(board)) {
            return ResponseEntity.ok(taskService.findTaskById(null, id, taskId));
        }
        Claims claims;

        if (isMicrosoftToken(token)) {
            claims = taskService.validateMicrosoftTokenAndAuthorization(token, id);
        } else {
            claims = taskService.validateToken(token);
        }

        taskService.validateOwnership(claims, id);

        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(claims, id, taskId));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody(required = false) @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable String id,
                                                             @RequestHeader(value = "Authorization") String token) {
        Board board = taskService.validateBoardAndOwnership(id, token);

        if (taskDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO, id));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponseWithAttachmentsDTO> updateTask(
            @PathVariable String id,
            @ModelAttribute @Valid TaskUpdateRequestDTO taskDTO,
            @PathVariable Integer taskId,
            @RequestHeader(value = "Authorization") String token,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Board board = taskService.validateBoardAndOwnership(id, token);

        if (taskDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO, taskId, files));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable String id, @PathVariable Integer taskId,
                                                      @RequestHeader(value = "Authorization") String token) {
        Board board = taskService.validateBoardAndOwnership(id, token);

        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id, taskId));
    }

    @GetMapping("/{id}/tasks/{taskId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id, @PathVariable Integer taskId,
                                                 @RequestParam String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);

        Resource resource = fileService.loadFileAsResource(id, taskId, fileName);

        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        MediaType mediaType;
        switch (fileExtension) {
            case "jpg":
            case "jpeg":
                mediaType = MediaType.IMAGE_JPEG;
                break;
            case "png":
                mediaType = MediaType.IMAGE_PNG;
                break;
            case "pdf":
                mediaType = MediaType.APPLICATION_PDF;
                break;
            default:
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                break;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/tasks/{taskId}/delete")
    public ResponseEntity<String> deleteFile(@PathVariable String id, @PathVariable Integer taskId,
                                             @RequestParam String url) {
        String actualFileName = url.substring(url.lastIndexOf("/") + 1);
        attachmentService.deleteAttachmentAndFile(id, taskId, actualFileName);
        return ResponseEntity.ok("delete file successfully");
    }

}