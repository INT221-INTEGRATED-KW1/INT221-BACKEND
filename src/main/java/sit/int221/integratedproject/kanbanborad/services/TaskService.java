package sit.int221.integratedproject.kanbanborad.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;

import java.util.List;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ListMapper listMapper;

    public List<TaskResponseDTO> findAllTask() {
        List<Task> tasks = taskRepository.findAll();
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public Task findTaskById(Integer id) {
        return taskRepository.findById(id).orElseThrow(
                () -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!")
        );
    }

}
