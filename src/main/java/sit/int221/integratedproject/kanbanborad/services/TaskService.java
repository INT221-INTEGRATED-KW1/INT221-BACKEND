package sit.int221.integratedproject.kanbanborad.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAllResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskByIdResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ListMapper listMapper;

    public List<TaskAllResponseDTO> findAllTask() {
        List<Task> tasks = taskRepository.findAll();
        return listMapper.mapList(tasks, TaskAllResponseDTO.class);
    }

    public TaskByIdResponseDTO findTaskById(Integer id) {
        Optional<Task> task = taskRepository.findById(id);
        if (!task.isPresent()) {
            throw new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!");
        }
        return modelMapper.map(task.get(), TaskByIdResponseDTO.class);
    }
}
