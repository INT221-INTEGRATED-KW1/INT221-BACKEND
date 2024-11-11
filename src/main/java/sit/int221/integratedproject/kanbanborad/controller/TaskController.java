package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskUpdateRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Attachment;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.AttachmentRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.services.FileService;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.StatusService;
import sit.int221.integratedproject.kanbanborad.services.TaskService;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = "http://localhost")
public class TaskController {
    private final TaskService taskService;
    private final BoardRepository boardRepository;
    private final StatusService statusService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CollaboratorRepository collaboratorRepository;
    private final FileService fileService;
    private final AttachmentRepository attachmentRepository;

    public TaskController(TaskService taskService, BoardRepository boardRepository, StatusService statusService, JwtTokenUtil jwtTokenUtil, CollaboratorRepository collaboratorRepository, FileService fileService, AttachmentRepository attachmentRepository) {
        this.taskService = taskService;
        this.boardRepository = boardRepository;
        this.statusService = statusService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.collaboratorRepository = collaboratorRepository;
        this.fileService = fileService;
        this.attachmentRepository = attachmentRepository;
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask(@RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String[] filterStatuses,
                                                            @PathVariable String id,
                                                            @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = getBoardOrThrow(id);

        List<TaskResponseDTO> tasks = fetchTasksBasedOnParams(sortBy, filterStatuses, id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(tasks);
        }

        Claims claims = validateToken(token);

        validateOwnership(claims, id);

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
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(taskService.findTaskById(null, id, taskId));
        }

        Claims claims = validateToken(token);

        validateOwnership(claims, id);

        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(claims, id, taskId));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody(required = false) @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable String id,
                                                             @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

        if (taskDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO, id));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(
            @PathVariable String id,
            @ModelAttribute @Valid TaskUpdateRequestDTO taskDTO,
            @PathVariable Integer taskId,
            @RequestHeader(value = "Authorization") String token,
            @RequestParam("files") List<MultipartFile> files) {
        Board board = validateBoardAndOwnership(id, token);

        if (taskDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO, taskId, files));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable String id, @PathVariable Integer taskId,
                                                      @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

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

        String dbFileName = id + "_" + taskId + "_" + actualFileName;

        fileService.deleteFile(id, taskId, actualFileName);

        Optional<Attachment> attachment = attachmentRepository.findByTaskIdAndFilename(taskId, dbFileName);
        if (attachment.isPresent()) {
            attachmentRepository.delete(attachment.get());
        } else {
            throw new ItemNotFoundException("Attachment not found for file: " + dbFileName);
        }

        return ResponseEntity.ok("delete file successfully");
    }

    @GetMapping("/test")
    public ResponseEntity<Object> testPropertiesMapping() {
        return ResponseEntity.ok(fileService.getFileStorageLocation() + " has been created !!!");
    }

    private Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private Claims validateToken(String token) {
        return Utils.getClaims(token, jwtTokenUtil);
    }

    private void validateOwnership(Claims claims, String boardId) {
        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }

        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        if (collaborator.isPresent() && collaborator.get().getStatus() == CollabStatus.PENDING) {
            throw new ForbiddenException("You cannot access this board because your invitation is pending.");
        }
    }

    private boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    private Claims validateTokenAndOwnership(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = token.substring(7);
        Claims claims;
        try {
            claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get JWT Token");
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
        }

        validateOwnership(claims, boardId);

        if (!isOwner((String) claims.get("oid"), boardId) && !hasWriteAccess((String) claims.get("oid"), boardId)) {
            throw new ForbiddenException("You are not allowed to modify this board.");
        }

        return claims;
    }

    private Board validateBoardAndOwnership(String boardId, String token) {
        Board board = getBoardOrThrow(boardId);
        Claims claims = validateTokenAndOwnership(token, boardId);
        return board;
    }

    private boolean isCollaborator(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

    private boolean hasWriteAccess(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);

        if (collaborator.isPresent()) {
            return collaborator.get().getAccessRight().equalsIgnoreCase("WRITE");
        }

        return false;
    }

}