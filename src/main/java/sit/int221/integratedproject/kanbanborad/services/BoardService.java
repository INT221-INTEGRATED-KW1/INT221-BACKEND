package sit.int221.integratedproject.kanbanborad.services;

import ch.qos.logback.core.net.server.Client;
import io.jsonwebtoken.Claims;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.BoardResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.OwnerResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoardService {
    private final BoardRepository boardRepository;
    private final ListMapper listMapper;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public BoardService(BoardRepository boardRepository, ListMapper listMapper, ModelMapper modelMapper, UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.boardRepository = boardRepository;
        this.listMapper = listMapper;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public List<Board> getAllBoard() {
        return boardRepository.findAll();
    }

    public BoardResponseDTO getBoardById(Claims claims, String id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        String oid = (String) claims.get("oid");
        User user = userRepository.findById(oid)
                .orElseThrow(() -> new ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));

        return getBoardResponseDTO(user, board);
    }

    public BoardResponseDTO createBoard(Claims claims, BoardRequestDTO boardRequestDTO) {
        String oid = (String) claims.get("oid");
        User user = userRepository.findById(oid)
                .orElseThrow(() -> new  ItemNotFoundException("User Id " + oid + " DOES NOT EXIST !!!"));
        Board board = new Board();
        board.setOid(oid);
        board.setName(boardRequestDTO.getName());
        Board savedBoard = boardRepository.save(board);

        return getBoardResponseDTO(user, savedBoard);
    }

    private BoardResponseDTO getBoardResponseDTO(User user, Board savedBoard) {
        OwnerResponseDTO ownerResponseDTO = new OwnerResponseDTO();
        ownerResponseDTO.setOid(user.getOid());
        ownerResponseDTO.setName(user.getName());

        BoardResponseDTO boardResponseDTO = new BoardResponseDTO();
        boardResponseDTO.setId(savedBoard.getId());
        boardResponseDTO.setName(Utils.trimString(savedBoard.getName()));
        boardResponseDTO.setOwner(ownerResponseDTO);

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
}
