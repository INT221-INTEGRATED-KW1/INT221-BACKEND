package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardVisibilityRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final BoardRepository boardRepository;
    private final ListMapper listMapper;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final StatusRepository statusRepository;
    private final CollaboratorRepository collaboratorRepository;

    public BoardService(BoardRepository boardRepository, ListMapper listMapper, ModelMapper modelMapper, UserRepository userRepository, JwtTokenUtil jwtTokenUtil, StatusRepository statusRepository, CollaboratorRepository collaboratorRepository) {
        this.boardRepository = boardRepository;
        this.listMapper = listMapper;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.statusRepository = statusRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public List<CollaboratorResponseDTO> getCollaborators(String boardId) {
        // Fetch collaborators without checking access (for public boards)
        return collaboratorRepository.findByBoardId(boardId).stream()
                .map(this::convertToCollaboratorDTO)
                .collect(Collectors.toList());
    }

    public List<CollaboratorResponseDTO> getCollaborators(String boardId, Claims claims) {
        // Fetch board and ensure access is granted
        Board board = getBoardOrThrow(boardId);
        validateAccess(claims, board); // This ensures that the user has access to the board

        // Fetch collaborators and map to DTO
        return collaboratorRepository.findByBoardId(boardId).stream()
                .map(this::convertToCollaboratorDTO)
                .collect(Collectors.toList());
    }

    public CollaboratorResponseDTO getCollaboratorById(String boardId, String collabOid) {
        // Fetch collaborator without checking access (for public boards)
        Collaborator collaborator = collaboratorRepository.findByBoardIdAndOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        return convertToCollaboratorDTO(collaborator);
    }

    public CollaboratorResponseDTO getCollaboratorById(String boardId, String collabOid, Claims claims) {
        // Fetch board and ensure access is granted
        Board board = getBoardOrThrow(boardId);
        validateAccess(claims, board); // Validate that the user has access to the board

        // Fetch collaborator and throw exception if not found
        Collaborator collaborator = collaboratorRepository.findByBoardIdAndOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        return convertToCollaboratorDTO(collaborator);
    }

    public BoardsResponseDTO getAllBoards(Claims claims) {
        String oid = (String) claims.get("oid");

        User user = userRepository.findById(oid)
                .orElseThrow(() -> new ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));

        // ดึง personal boards
        List<Board> personalBoards = boardRepository.findByOid(oid);

        // ดึง collab boards
        List<Board> collabBoards = boardRepository.findCollaboratorBoardsByUserOid(oid);

        // แปลง personalBoards และ collabBoards เป็น BoardResponseDTO พร้อมดึงข้อมูล owner
        List<BoardResponseDTO> personalBoardDTOs = personalBoards.stream()
                .map(board -> new BoardResponseDTO(
                        board.getId(),
                        board.getName(),
                        convertToOwnerDTO(user), // ดึงข้อมูล owner จาก User
                        board.getVisibility()))
                .collect(Collectors.toList());

        List<BoardResponseDTO> collabBoardDTOs = collabBoards.stream()
                .map(board -> new BoardResponseDTO(
                        board.getId(),
                        board.getName(),
                        convertToOwnerDTO(userRepository.findById(board.getOid())
                                .orElseThrow(() -> new ItemNotFoundException("Owner not found!"))), // ดึงข้อมูล owner ของ collab board
                        board.getVisibility()))
                .collect(Collectors.toList());

        return new BoardsResponseDTO(personalBoardDTOs, collabBoardDTOs);
    }

    private OwnerResponseDTO convertToOwnerDTO(User user) {
        return new OwnerResponseDTO(user.getOid(), user.getName());
    }

    public BoardResponseDTO getBoardById(String id) {
        // Check if the Board exists
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        User user = userRepository.findById(board.getOid())
                .orElseThrow(() -> new ItemNotFoundException("User Id " + board.getOid() + " DOES NOT EXIST !!!"));

        return getBoardResponseDTO(user, board);  // Convert and return DTO for public boards
    }

    public BoardResponseDTO getBoardById(String id, Claims claims) {
        // Check if the Board exists
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        // Validate ownership or access using claims
        validateOwnership(claims, id);

        User user = userRepository.findById(board.getOid())
                .orElseThrow(() -> new ItemNotFoundException("User Id " + board.getOid() + " DOES NOT EXIST !!!"));

        return getBoardResponseDTO(user, board);  // Convert and return DTO for private boards
    }

    public BoardResponseDTO createBoard(Claims claims, BoardRequestDTO boardRequestDTO) {
        String oid = (String) claims.get("oid");
        User user = userRepository.findById(oid)
                .orElseThrow(() -> new ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));

        // Create and save a new board
        Board board = new Board();
        board.setOid(oid);
        board.setName(boardRequestDTO.getName());
        board.setLimitMaximumStatus(false);
        board.setVisibility("PRIVATE");
        Board savedBoard = boardRepository.save(board);

        // Save default statuses for the newly created board
        saveDefaultStatuses(savedBoard);

        // Return the BoardResponseDTO
        return getBoardResponseDTO(user, savedBoard);
    }

    private void saveDefaultStatuses(Board board) {
        List<Status> defaultStatuses = List.of(
                new Status("No Status", "The default status", "gray", board),
                new Status("To Do", null, "orange", board),
                new Status("Doing", "Being worked on", "blue", board),
                new Status("Done", "Finished", "green", board)
        );

        // Save all default statuses
        statusRepository.saveAll(defaultStatuses);
    }

    private BoardResponseDTO getBoardResponseDTO(User user, Board savedBoard) {
        OwnerResponseDTO ownerResponseDTO = new OwnerResponseDTO();
        ownerResponseDTO.setOid(user.getOid());
        ownerResponseDTO.setName(user.getName());

        BoardResponseDTO boardResponseDTO = new BoardResponseDTO();
        boardResponseDTO.setId(savedBoard.getId());
        boardResponseDTO.setName(Utils.trimString(savedBoard.getName()));
        boardResponseDTO.setOwner(ownerResponseDTO);
        boardResponseDTO.setVisibility(savedBoard.getVisibility());

        return boardResponseDTO;
    }

    @Transactional
    public StatusLimitResponseDTO updateBoardLimit(String id, BoardLimitRequestDTO boardDTO) {
        Board existingBoard = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        existingBoard.setLimitMaximumStatus(boardDTO.getLimitMaximumStatus());
        Board updatedBoard = boardRepository.save(existingBoard);

        StatusLimitResponseDTO responseDTO = new StatusLimitResponseDTO();
        responseDTO.setId(updatedBoard.getId());
        responseDTO.setStatusLimit(updatedBoard.getLimitMaximumStatus());

        if (updatedBoard.getLimitMaximumStatus()) {
            List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
            for (Status status : updatedBoard.getStatuses()) {
                if (status.getTasks().size() > Utils.MAX_SIZE) {
                    StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
                    statusResponseDTO.setNoOfTasks(status.getTasks().size());
                    statusResponseDTOs.add(statusResponseDTO);
                }
            }
            responseDTO.setStatuses(statusResponseDTOs);
        }

        return responseDTO;
    }

    @Transactional
    public BoardVisibilityResponseDTO updateBoardVisibility(String id, BoardVisibilityRequestDTO boardDTO) {
        if (!boardDTO.getVisibility().equalsIgnoreCase("PUBLIC")
                && !boardDTO.getVisibility().equalsIgnoreCase("PRIVATE")) {
            throw new BadRequestException("Visibility must be either 'public' or 'private'.");
        }
        Board existingBoard = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
            existingBoard.setVisibility(boardDTO.getVisibility());
        Board updatedBoard = boardRepository.save(existingBoard);

        BoardVisibilityResponseDTO responseDTO = new BoardVisibilityResponseDTO();
        responseDTO.setVisibility(updatedBoard.getVisibility());

        return responseDTO;
    }

    private Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private void validateAccess(Claims claims, Board board) {
        String oid = (String) claims.get("oid");
        if (!isPublicBoard(board) && !isOwner(oid, board.getId()) && !isCollaborator(oid, board)) {
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

    private boolean isCollaborator(String oid, Board board) {
        return collaboratorRepository.findByBoardIdAndOid(board.getId(), oid).isPresent();
    }

    private CollaboratorResponseDTO convertToCollaboratorDTO(Collaborator collaborator) {
        CollaboratorResponseDTO dto = new CollaboratorResponseDTO();
        dto.setOid(collaborator.getOid());
        dto.setName(collaborator.getName());
        dto.setEmail(collaborator.getEmail());
        dto.setAccessRight(collaborator.getAccessRight());
        dto.setAddedOn(collaborator.getAddedOn());
        return dto;
    }

    private void validateOwnership(Claims claims, String boardId) {
        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }
    }

    private boolean isCollaborator(String oid, String boardId) {
        // เช็คจาก database ว่าผู้ใช้มีสิทธิ์เป็น collaborator หรือไม่
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

}