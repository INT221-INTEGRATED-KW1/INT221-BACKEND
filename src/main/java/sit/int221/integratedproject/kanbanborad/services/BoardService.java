package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Board;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.BoardRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoardService {
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ListMapper listMapper;

    public List<StatusLimitResponseDTO> findAllBoard() {
        List<Board> boards = boardRepository.findAll();
        return listMapper.mapList(boards, StatusLimitResponseDTO.class);
    }

    public StatusLimitResponseDTO findBoardById(Integer id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        return modelMapper.map(board, StatusLimitResponseDTO.class);
    }

    @Transactional
    public StatusLimitResponseDTO updateBoardLimit(Integer id, BoardRequestDTO boardDTO) {
        Board existingBoard = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        existingBoard.setLimitMaximumStatus(boardDTO.getLimitMaximumStatus());
        Board updatedBoard = boardRepository.save(existingBoard);

        StatusLimitResponseDTO responseDTO = new StatusLimitResponseDTO();
        responseDTO.setId(updatedBoard.getId());
        responseDTO.setLimitMaximumStatus(updatedBoard.getLimitMaximumStatus());

        if (updatedBoard.getLimitMaximumStatus()) {
            List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
            for (Status status : updatedBoard.getStatuses()) {
                if (status.getTasks().size() >= Utils.MAX_SIZE) {
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
