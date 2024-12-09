
package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardVisibilityRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.UserOwnRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final BoardRepository boardRepository;
    private final ModelMapper modelMapper;
    private final StatusRepository statusRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserOwnRepository userOwnRepository;

    public BoardService(BoardRepository boardRepository,  ModelMapper modelMapper, StatusRepository statusRepository, CollaboratorRepository collaboratorRepository, UserOwnRepository userOwnRepository) {
        this.boardRepository = boardRepository;
        this.modelMapper = modelMapper;
        this.statusRepository = statusRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userOwnRepository = userOwnRepository;
    }

    public List<CollaboratorResponseDTO> getCollaborators(String boardId) {
        return collaboratorRepository.findByBoardId(boardId).stream()
                .map(this::convertToCollaboratorDTO)
                .collect(Collectors.toList());
    }

    public List<CollaboratorResponseDTO> getCollaborators(String boardId, Claims claims) {
        Board board = getBoardOrThrow(boardId);
        validateAccess(claims, board);

        return collaboratorRepository.findByBoardId(boardId).stream()
                .map(this::convertToCollaboratorDTO)
                .collect(Collectors.toList());
    }

    public CollaboratorResponseDTO getCollaboratorById(String boardId, String collabOid) {
        Collaborator collaborator = collaboratorRepository.findByBoardIdAndOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        return convertToCollaboratorDTO(collaborator);
    }

    public CollaboratorResponseDTO getCollaboratorById(String boardId, String collabOid, Claims claims) {
        Board board = getBoardOrThrow(boardId);
        validateAccess(claims, board);

        Collaborator collaborator = collaboratorRepository.findByBoardIdAndOid(boardId, collabOid)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        return convertToCollaboratorDTO(collaborator);
    }

    public BoardsResponseDTO getAllBoards(Claims claims) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);

        UserOwn user = userOwnRepository.findById(oid)
                .orElseThrow(() -> new ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));

        List<Board> personalBoards = boardRepository.findByOid(oid);

        List<Board> collabBoards = boardRepository.findCollaboratorBoardsByUserOid(oid);

        List<BoardResponseDTO> personalBoardDTOs = personalBoards.stream()
                .map(board -> {
                    return new BoardResponseDTO(
                            board.getId(),
                            board.getName(),
                            convertToOwnerDTO(user),
                            board.getVisibility());
                }).collect(Collectors.toList());

        List<CollabBoardResponseDTO> collabBoardDTOs = collabBoards.stream()
                .map(board -> {
                    String accessRight = collaboratorRepository.findByOidAndBoardId(oid, board.getId())
                            .map(Collaborator::getAccessRight)
                            .orElse("No access right");

                    String invitationStatus = String.valueOf(collaboratorRepository.findByOidAndBoardId(oid, board.getId())
                            .map(Collaborator::getStatus)
                            .orElse(CollabStatus.PENDING));

                    return new CollabBoardResponseDTO(
                            board.getId(),
                            board.getName(),
                            convertToOwnerDTO(userOwnRepository.findById(board.getOid())
                                    .orElseThrow(() -> new ItemNotFoundException("Owner not found!"))),
                            board.getVisibility(),
                            accessRight,
                            invitationStatus
                    );
                })
                .collect(Collectors.toList());

        return new BoardsResponseDTO(personalBoardDTOs, collabBoardDTOs);
    }

    private OwnerResponseDTO convertToOwnerDTO(UserOwn user) {
        return new OwnerResponseDTO(user.getOid(), user.getName());
    }

    public BoardResponseDTO getBoardById(String id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        UserOwn user = userOwnRepository.findById(board.getOid())
                .orElseThrow(() -> new ItemNotFoundException("User Id " + board.getOid() + " DOES NOT EXIST !!!"));

        return getBoardResponseDTO(user, board);
    }

    public BoardResponseDTO getBoardById(String id, Claims claims) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        validateOwnership(claims, id);

        UserOwn user = userOwnRepository.findById(board.getOid())
                .orElseThrow(() -> new ItemNotFoundException("User Id " + board.getOid() + " DOES NOT EXIST !!!"));

        return getBoardResponseDTO(user, board);
    }

    public BoardResponseDTO createBoard(Claims claims, BoardRequestDTO boardRequestDTO) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        UserOwn user = userOwnRepository.findById(oid)
                .orElseThrow(() -> new ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));

        Board board = new Board();
        board.setOid(oid);
        board.setName(boardRequestDTO.getName());
        board.setLimitMaximumStatus(false);
        board.setVisibility("PRIVATE");
        Board savedBoard = boardRepository.save(board);

        saveDefaultStatuses(savedBoard);

        return getBoardResponseDTO(user, savedBoard);
    }

    private void saveDefaultStatuses(Board board) {
        List<Status> defaultStatuses = List.of(
                new Status("No Status", "The default status", "gray", board),
                new Status("To Do", null, "orange", board),
                new Status("Doing", "Being worked on", "blue", board),
                new Status("Done", "Finished", "green", board)
        );

        statusRepository.saveAll(defaultStatuses);
    }

    private BoardResponseDTO getBoardResponseDTO(UserOwn user, Board savedBoard) {
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
        if (boardDTO == null) {
            throw new BadRequestException("Request body cannot be null.");
        }
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

    public Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private void validateAccess(Claims claims, Board board) {
        String userId = JwtTokenUtil.getOidFromClaims(claims);

        if (!isPublicBoard(board) && !isOwner(userId, board.getId())) {
            Optional<Collaborator> collaboratorOpt = collaboratorRepository.findByOidAndBoardId(userId, board.getId());

            if (collaboratorOpt.isEmpty() || collaboratorOpt.get().getStatus() == CollabStatus.PENDING) {
                throw new ForbiddenException("You are not allowed to access this board.");
            }
        }
    }

    public boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }

    public boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    private CollaboratorResponseDTO convertToCollaboratorDTO(Collaborator collaborator) {
        CollaboratorResponseDTO dto = new CollaboratorResponseDTO();
        dto.setOid(collaborator.getOid());
        dto.setName(collaborator.getName());
        dto.setEmail(collaborator.getEmail());
        dto.setAccessRight(collaborator.getAccessRight());
        dto.setAddedOn(collaborator.getAddedOn());
        dto.setInvitationStatus(collaborator.getStatus().name());
        return dto;
    }

    private void validateOwnership(Claims claims, String boardId) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);

        if (isOwner(oid, boardId)) {
            return;
        }

        if (isCollaborator(oid, boardId)) {
            Collaborator collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId)
                    .orElseThrow(() -> new ForbiddenException("You are not allowed to access this board."));

            if (collaborator.getStatus() == CollabStatus.PENDING) {
                throw new ForbiddenException("You cannot access this board because your invitation is pending.");
            }
            return;
        }

        throw new ForbiddenException("You are not allowed to access this board.");
    }

    private boolean isCollaborator(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

    public Board validateBoardAndOwnership(String boardId, Claims claims) {
        Board board = getBoardOrThrow(boardId);
        String oid = JwtTokenUtil.getOidFromClaims(claims);

        if (!isOwner(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }
        return board;
    }

}
