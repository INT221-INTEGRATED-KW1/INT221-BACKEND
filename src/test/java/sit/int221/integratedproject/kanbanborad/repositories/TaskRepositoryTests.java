package sit.int221.integratedproject.kanbanborad.repositories;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.models.Status;

import java.util.List;
import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TaskRepositoryTests {
    @Autowired
    private TaskRepository taskRepository;

    @Test
    public void TaskRepository_SaveAll_ReturnSavedTask() {
        Task task = Task.builder().title("   Initialize    ").description("  t  ")
                .assignees("t").status(Status.NO_STATUS).build();

        Task savedPokemon = taskRepository.save(task);

        Assertions.assertThat(savedPokemon).isNotNull();
        Assertions.assertThat(savedPokemon.getId()).isGreaterThan(0);
    }

    @Test
    public void TaskRepository_GetAll_ReturnMoreThanOneTask() {
        Task task = Task.builder().title("   Initialize    ").description("  t  ")
                .assignees("t").status(Status.NO_STATUS).build();

        taskRepository.save(task);

        List<Task> tasks = taskRepository.findAll();

        Assertions.assertThat(tasks).isNotNull();
        Assertions.assertThat(tasks.size()).isEqualTo(5);
    }

    @Test
    public void TaskRepository_FindById_ReturnTask() {
        Task task = Task.builder().title("   Initialize    ").description("  t  ")
                .assignees("t").status(Status.NO_STATUS).build();

        taskRepository.save(task);

        Task foundTask = taskRepository.findById(task.getId()).get();

        Assertions.assertThat(foundTask).isNotNull();
    }

    @Test
    public void TaskRepository_UpdateTask_ReturnTaskNotNull() {
        Task task = Task.builder().title("   Initialize    ").description("  t  ")
                .assignees("t").status(Status.NO_STATUS).build();

        taskRepository.save(task);

        Task taskSave = taskRepository.findById(task.getId()).get();
        taskSave.setTitle("Initialize Project");
        taskSave.setDescription("Create Spring-boot project & Setup Dev Environment");
        taskSave.setAssignees("Carry; test");
        taskSave.setStatus(Status.DOING);

        Task updatedTask = taskRepository.save(taskSave);

        Assertions.assertThat(updatedTask.getTitle()).isNotNull();
        Assertions.assertThat(updatedTask.getDescription()).isNotNull();
        Assertions.assertThat(updatedTask.getAssignees()).isNotNull();
        Assertions.assertThat(updatedTask.getStatus()).isNotNull();
    }

    @Test
    public void TaskRepository_TaskDelete_ReturnTaskIsEmpty() {
        Task task = Task.builder().title("   Initialize    ").description("  t  ")
                .assignees("t").status(Status.NO_STATUS).build();

        taskRepository.save(task);

        taskRepository.deleteById(task.getId());
        Optional<Task> taskReturn = taskRepository.findById(task.getId());

        Assertions.assertThat(taskReturn).isEmpty();
    }

}
