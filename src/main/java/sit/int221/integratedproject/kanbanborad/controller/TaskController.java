package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
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

    public TaskController(TaskService taskService, BoardRepository boardRepository, StatusService statusService, JwtTokenUtil jwtTokenUtil, CollaboratorRepository collaboratorRepository) {
        this.taskService = taskService;
        this.boardRepository = boardRepository;
        this.statusService = statusService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.collaboratorRepository = collaboratorRepository;
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
        System.out.println("test1");

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
        System.out.println("test2");

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
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(@PathVariable String id,
                                                             @RequestBody(required = false) @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable Integer taskId,
                                                             @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

        if (taskDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO, taskId));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable String id, @PathVariable Integer taskId,
                                                      @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id, taskId));
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

        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId) && !hasWriteAccess(oid, boardId)) {
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
        // เช็คจาก database ว่าผู้ใช้มีสิทธิ์เป็น collaborator หรือไม่
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

    private boolean hasWriteAccess(String oid, String boardId) {
        // Fetch collaborator and ensure they have WRITE access
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);

        if (collaborator.isPresent()) {
            return collaborator.get().getAccessRight().equalsIgnoreCase("WRITE");
        }

        return false;
    }

}