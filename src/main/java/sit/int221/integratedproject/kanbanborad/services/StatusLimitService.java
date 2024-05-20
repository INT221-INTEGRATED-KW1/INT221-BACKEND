package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.StatusLimit;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.StatusLimitRepository;
import sit.int221.integratedproject.kanbanborad.repositories.StatusRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatusLimitService {
    @Autowired
    private StatusLimitRepository statusLimitRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private StatusRepository statusRepository;

    public List<StatusLimitResponseDTO> findAllStatusLimit() {
        List<StatusLimit> statusLimits = statusLimitRepository.findAll();
        List<StatusLimitResponseDTO> responseDTOs = new ArrayList<>();
        for (StatusLimit existingStatusLimit : statusLimits) {
            StatusLimitResponseDTO responseDTO = mapToStatusLimitResponseDTO(existingStatusLimit);
            responseDTOs.add(responseDTO);
        }
        return responseDTOs;
    }

    private StatusLimitResponseDTO mapToStatusLimitResponseDTO(StatusLimit existingStatusLimit) {
        StatusLimitResponseDTO responseDTO = modelMapper.map(existingStatusLimit, StatusLimitResponseDTO.class);
        if (existingStatusLimit.getStatusLimit()) {
            List<StatusResponseDetailDTO> statusResponseDTOs = getStatusResponseDTOs();
            responseDTO.setStatuses(statusResponseDTOs);
        }
        return responseDTO;
    }

    private List<StatusResponseDetailDTO> getStatusResponseDTOs() {
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        List<Status> statuses = statusRepository.findAll();
        for (Status status : statuses) {
            if (status.getTasks().size() >= Utils.MAX_SIZE) {
                StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
                statusResponseDTO.setNoOfTasks(status.getTasks().size());
                statusResponseDTOs.add(statusResponseDTO);
            }
        }
        return statusResponseDTOs;
    }

    public StatusLimitResponseDTO findStatusLimitById(Integer id) {
        StatusLimit statusLimit = statusLimitRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("StatusLimit Id " + id + " DOES NOT EXIST !!!"));
        return modelMapper.map(statusLimit, StatusLimitResponseDTO.class);
    }

    @Transactional
    public StatusLimitResponseDTO updateStatusLimit(Integer id, StatusLimitRequestDTO statusLimitRequestDTO) {
        StatusLimit existingStatusLimit = statusLimitRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("StatusLimit Id " + id + " DOES NOT EXIST !!!"));
        existingStatusLimit.setStatusLimit(statusLimitRequestDTO.getStatusLimit());
        StatusLimit updatedStatusLimit = statusLimitRepository.save(existingStatusLimit);

        StatusLimitResponseDTO responseDTO = mapToStatusLimitResponseDTO(updatedStatusLimit);
        return responseDTO;
    }
}
