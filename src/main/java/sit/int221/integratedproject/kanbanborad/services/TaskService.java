package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskUpdateRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.*;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.FieldNotFoundException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.*;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final StatusRepository statusRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final BoardRepository boardRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final FileService fileService;
    private final AttachmentRepository attachmentRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public TaskService(TaskRepository taskRepository, StatusRepository statusRepository, ModelMapper modelMapper, ListMapper listMapper, BoardRepository boardRepository, CollaboratorRepository collaboratorRepository, FileService fileService, AttachmentRepository attachmentRepository, JwtTokenUtil jwtTokenUtil) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.boardRepository = boardRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.fileService = fileService;
        this.attachmentRepository = attachmentRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public List<TaskResponseDTO> findAllTask(String id) {
        Board board = getBoardById(id);
        List<Task> tasks = board.getTasks();
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSorted(String sortBy, String id) {
        Board board = getBoardById(id);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardId(board.getId(), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskFiltered(String[] filterStatuses, String id) {
        Board board = getBoardById(id);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(board.getId(), Arrays.asList(filterStatuses));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSortedAndFiltered(String sortBy, String[] filterStatuses, String id) {
        Board board = getBoardById(id);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(board.getId(), Arrays.asList(filterStatuses), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(Claims claims, String boardId, Integer taskId) {
        findBoardByIdAndValidateAccess(claims, boardId);

        Task task = taskRepository.findTaskByIdAndBoardId(taskId, boardId);
        if (task == null) {
            throw new ItemNotFoundException("Task Id " + taskId + " does not belong to Board Id " + boardId);
        }

        TaskDetailResponseDTO taskDetailResponseDTO = modelMapper.map(task, TaskDetailResponseDTO.class);

        List<AttachmentResponseDTO> attachmentDTOs = task.getAttachments().stream()
                .map(attachment -> {
                    String originalFilename = attachment.getFilename();
                    String trimmedFilename = originalFilename.replaceFirst("^" + boardId + "_" + taskId + "_", "");

                    return new AttachmentResponseDTO(
                            attachment.getId(),
                            trimmedFilename, // ใช้ชื่อไฟล์ที่ถูกตัดแล้ว
                            attachment.getFilePath(),
                            attachment.getFileSize(),
                            attachment.getCreatedOn()
                    );
                })
                .collect(Collectors.toList());

        taskDetailResponseDTO.setAttachments(attachmentDTOs);

        int currentAttachmentCount = attachmentRepository.countByTaskId(taskId);
        taskDetailResponseDTO.setAttachmentCount(currentAttachmentCount);

        return taskDetailResponseDTO;
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO, String id) {
        Status status = getStatusById(taskDTO.getStatus());
        Board board = getBoardById(id);
        validateStatusBelongsToBoard(status, board.getId());
        checkStatusLimit(status);

        Task task = new Task();
        populateTaskFromDTO(task, taskDTO, status, board);
        task.setAttachmentCount(0);
        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskResponseWithAttachmentsDTO updateTask(String id, TaskUpdateRequestDTO taskDTO, Integer taskId, List<MultipartFile> files) {
        Board board = getBoardById(id);
        Task existingTask = getTaskById(taskId);
        Status status = getStatusById(taskDTO.getStatus());
        validateStatusBelongsToBoard(status, board.getId());
        checkStatusLimit(status);

        populateTaskFromDTO(existingTask, taskDTO, status, board);

        List<Attachment> newAttachments = validateAndHandleAttachmentsWithMultipart(files, existingTask, id, taskId);
        existingTask.setAttachments(newAttachments);

        for (Attachment attachment : newAttachments) {
            attachment.setTask(existingTask);
            attachment.setFileSize((int) files.get(newAttachments.indexOf(attachment)).getSize());
            attachmentRepository.save(attachment);
        }

        int totalAttachmentsCount = attachmentRepository.countByTaskId(taskId);
        existingTask.setAttachmentCount(totalAttachmentsCount);

        Task updatedTask = taskRepository.save(existingTask);
        TaskResponseWithAttachmentsDTO responseDTO = modelMapper.map(updatedTask, TaskResponseWithAttachmentsDTO.class);

        List<AttachmentResponseDTO> modifiedAttachments = updatedTask.getAttachments().stream()
                .map(attachment -> {
                    String trimmedFilename = attachment.getFilename().replaceFirst("^" + id + "_" + taskId + "_", "");
                    return new AttachmentResponseDTO(
                            attachment.getId(),
                            trimmedFilename,
                            attachment.getFilePath(),
                            attachment.getFileSize(),
                            attachment.getCreatedOn()
                    );
                })
                .collect(Collectors.toList());

        responseDTO.setAttachments(modifiedAttachments); // เซ็ตค่า attachments

        return responseDTO;
    }

    @Transactional
    public TaskResponseDTO deleteTask(String id, Integer taskId) {
        Board board = getBoardById(id);
        Task taskToDelete = getTaskById(taskId);
        validateTaskBelongsToBoard(taskToDelete, board.getId());

        for (Attachment attachment : taskToDelete.getAttachments()) {
            fileService.deleteFile(id, taskId, attachment.getFilePath());
            attachmentRepository.delete(attachment);
        }

        taskRepository.deleteById(taskId);

        return modelMapper.map(taskToDelete, TaskResponseDTO.class);
    }

    private void validateSortField(String sortBy) {
        List<String> validFields = Arrays.asList("status.name");
        if (!validFields.contains(sortBy)) {
            throw new BadRequestException("Invalid filter parameter: " + sortBy);
        }
    }

    private void checkStatusLimit(Status status) {
        boolean isSpecialStatus = status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE);
        if (status.getBoard().getLimitMaximumStatus() && !isSpecialStatus && status.getTasks().size() >= Utils.MAX_SIZE) {
            throw new BadRequestException("Status limit exceeded.");
        }
    }

    private void populateTaskFromDTO(Task task, TaskRequestDTO taskDTO, Status status, Board board) {
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);
        task.setBoard(board);
    }

    private void populateTaskFromDTO(Task task, TaskUpdateRequestDTO taskDTO, Status status, Board board) {
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);
        task.setBoard(board);
    }

    private Board getBoardById(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private Task getTaskById(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!"));
    }

    private Status getStatusById(Integer statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new FieldNotFoundException("Status Id " + statusId + " DOES NOT EXIST !!!"));
    }

    private void validateTaskBelongsToBoard(Task task, String boardId) {
        if (!task.getBoard().getId().equals(boardId)) {
            throw new ItemNotFoundException("Task Id " + task.getId() + " does not belong to Board Id " + boardId);
        }
    }

    private void validateStatusBelongsToBoard(Status status, String boardId) {
        if (!status.getBoard().getId().equals(boardId)) {
            throw new BadRequestException("The status does not belong to the specified board.");
        }
    }

    public void findBoardByIdAndValidateAccess(Claims claims, String id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        if (!board.getVisibility().equalsIgnoreCase("PUBLIC")) {
            if (claims == null) {
                throw new ForbiddenException("Authentication required to access this board.");
            }

            String oid = JwtTokenUtil.getOidFromClaims(claims);

            if (!isOwner(oid, id)) {
                Optional<Collaborator> collaboratorOpt = collaboratorRepository.findByOidAndBoardId(oid, id);

                if (collaboratorOpt.isEmpty() || collaboratorOpt.get().getStatus() == CollabStatus.PENDING) {
                    throw new ForbiddenException("You are not allowed to access this board.");
                }
            }
        }
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    private List<Attachment> validateAndHandleAttachmentsWithMultipart(List<MultipartFile> files, Task task,
                                                                       String id, Integer taskId) {
        List<Attachment> attachments = new ArrayList<>();
        int MAX_FILES = 10;
        int MAX_FILE_SIZE_MB = 20;

        List<String> errorMessages = new ArrayList<>();
        List<String> notAddedFiles = new ArrayList<>();

        if (files != null) {
            int existingAttachmentCount = task.getAttachments().size();
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                if (fileName == null) {
                    continue;
                }

                String uniqueFilename = String.format("%s_%d_%s", id, taskId, fileName);

                if (task.getAttachments().stream().anyMatch(att -> att.getFilename().equals(uniqueFilename))) {
                    errorMessages.add("File with the same filename cannot be added. Please delete and add again to update the file.");
                    notAddedFiles.add(fileName);
                    continue;
                }

                if (file.getSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
                    errorMessages.add("Each file cannot be larger than " + MAX_FILE_SIZE_MB + " MB.");
                    notAddedFiles.add(fileName);
                    continue;
                }

                if (existingAttachmentCount + attachments.size() >= MAX_FILES) {
                    errorMessages.add("Each task can have at most " + MAX_FILES + " files.");
                    notAddedFiles.add(fileName);
                    continue;
                }

                String filePath = fileService.store(file, id, taskId);
                Attachment attachment = new Attachment(uniqueFilename, filePath);
                attachment.setFileSize((int) file.getSize());
                attachments.add(attachment);
            }
        }

        if (!errorMessages.isEmpty()) {
            throw new BadRequestException(String.join(" - ", errorMessages) +
                    "\nThe following files are not added: " + String.join(", ", notAddedFiles));
        }

        return attachments;
    }

    public Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    public Claims validateToken(String token) {
        return Utils.getClaims(token, jwtTokenUtil);
    }

    public void validateOwnership(Claims claims, String boardId) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }

        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        if (collaborator.isPresent() && collaborator.get().getStatus() == CollabStatus.PENDING) {
            throw new ForbiddenException("You cannot access this board because your invitation is pending.");
        }
    }

    public boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }

    public Claims validateTokenAndOwnership(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
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

    public Board validateBoardAndOwnership(String boardId, String token) {
        Board board = getBoardOrThrow(boardId);
        Claims claims;

        if (Utils.isMicrosoftToken(token)) {
            claims = validateMicrosoftTokenForModify(token, boardId);
        } else {
            claims = validateTokenAndOwnership(token, boardId);
        }
        return board;
    }

    public boolean isCollaborator(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

    public boolean hasWriteAccess(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);

        return collaborator.map(value -> value.getAccessRight().equalsIgnoreCase("WRITE")).orElse(false);
    }

    public Claims validateMicrosoftTokenForModify(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
        Claims claims;
        try {
            claims = Utils.extractClaimsFromMicrosoftToken(jwtToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Microsoft Token");
        }

        String oid = jwtTokenUtil.getOidFromToken(jwtToken);

        if (!isOwner(oid, boardId) && !hasWriteAccess(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to modify this board.");
        }

        return claims;
    }

    public Claims validateMicrosoftTokenAndAuthorization(String token, String boardId) {
        Claims claims = Utils.extractClaimsFromMicrosoftToken(token);

        String oid = jwtTokenUtil.getOidFromToken(token);

        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }

        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        if (collaborator.isPresent() && collaborator.get().getStatus() == CollabStatus.PENDING) {
            throw new ForbiddenException("You cannot access this board because your invitation is pending.");
        }

        return claims;
    }

}