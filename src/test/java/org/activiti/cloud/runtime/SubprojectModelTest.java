package org.activiti.cloud.runtime;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.activiti.cloud.runtime.ModelsContextTest.SUBPROJECT_MODEL_DEFINITION_KEY;
import static org.activiti.cloud.runtime.ModelsContextTest.FILE_MODEL_DEFINITION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest()
@DirtiesContext
public class SubprojectModelTest {

    private final Logger logger = LoggerFactory.getLogger(SubprojectModelTest.class);

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private SecurityManager securityManager;

    private ProcessInstance processInstance;

    @BeforeEach
    public void processStart() {
        securityUtil.logInAs("bob");
        String initiator_group = securityManager.getAuthenticatedUserGroups().get(0);
        logger.info("> User In Group " + initiator_group);
        this.processInstance = processRuntime.start(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(SUBPROJECT_MODEL_DEFINITION_KEY)
                .withVariable("initiator_group", initiator_group)
                .withVariable("name", "My First Subproject")
                .build());
        logger.info("> Created Process Instance: " + this.processInstance);
    }

    @Test
    public void processStatusTest() {
        assertThat(this.processInstance.getStatus())
                .isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
    }

    @Test
    public void processVariablesTest() {
        List<VariableInstance> variables = processRuntime.variables(ProcessPayloadBuilder
                .variables()
                .withProcessInstance(processInstance)
                .build()
        );
        for (VariableInstance var : variables) {
            logger.info("> Found Process Instance Variable: " + var);
            switch (var.getName()) {
                case "name":
                    assertThat(var.getValue().toString()).isEqualTo("My First Subproject");
                    break;
                case "initiator_group":
                    assertThat(var.getValue().toString()).isEqualTo("activitiTeam");
                    break;
                case "close_subproject":
                    assertThat(var.getValue().toString()).isEqualTo("false");
                    break;
                case "creation_name":
                    assertThat(var.getValue().toString()).isEqualTo("<Безымянный>");
                    break;
            }
        }
    }

    @Test
    public void firstTaskStateTest() {
        Page<Task> tasks = taskRuntime.tasks(
                Pageable.of(0, 10),
                TaskPayloadBuilder.tasksForProcess(processInstance).build()
        );
        assertThat(tasks.getTotalItems()).isEqualTo(1);

        Task task = tasks.getContent().get(0);
        logger.info("> Available Task: " + task);

        assertThat(task.getName()).isEqualTo("Создать");
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);

        List<String> groupCandidates = taskRuntime.groupCandidates(task.getId());
        assertThat(groupCandidates.size()).isEqualTo(1);
        assertThat(groupCandidates).contains("activitiTeam");
    }

    @Test
    public void createFileTest() {
        Page<Task> tasks = taskRuntime.tasks(
                Pageable.of(0, 1),
                TaskPayloadBuilder.tasksForProcess(processInstance).build()
        );
        Task task = tasks.getContent().get(0);
        logger.info("> Available Task: " + task);

        // Начало выполнения задачи
        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim()
                .withTaskId(task.getId())
                .build()
        );
        logger.info("> Claimed Task: " + claimedTask);
        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
        assertThat(claimedTask.getAssignee()).isEqualTo("bob");

        // Усвоение имени создаваемому файлу
        processRuntime.setVariables(ProcessPayloadBuilder.setVariables(processInstance)
                .withVariable("creation_name", "My First File")
                .build()
        );

        // Выполнение задачи - создание файла
        Task completedTask = taskRuntime.complete(TaskPayloadBuilder.complete()
                .withTaskId(task.getId())
                .build()
        );
        logger.info("> Completed Task: " + completedTask);
        assertThat(completedTask.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(completedTask.getId()).isEqualTo(task.getId());

        // Получение новой задачи
        Page<Task> nextTasks = taskRuntime.tasks(
                Pageable.of(0, 2),
                TaskPayloadBuilder.tasksForProcess(processInstance).build()
        );
        assertThat(nextTasks.getTotalItems()).isEqualTo(1);
        Task nextTask = tasks.getContent().get(0);
        logger.info("> New Available Task: " + nextTask);

        // Проверка того, что это новая задача
        assertThat(nextTask.getName()).isEqualTo("Создать");
        assertThat(nextTask.getStatus()).isEqualTo(Task.TaskStatus.CREATED);

        // Проверка того, что файл создался
        Page<ProcessInstance> fileProcessInstancePage = processRuntime.processInstances(
                Pageable.of(0, 10),
                ProcessPayloadBuilder.subprocesses(processInstance)
        );
        assertThat(fileProcessInstancePage.getTotalItems()).isEqualTo(1);
        ProcessInstance fileProcessInstance = fileProcessInstancePage.getContent().get(0);
        logger.info("> Created File Process: " + fileProcessInstance);

        // Проверка того, что это действительно файл
        assertThat(fileProcessInstance.getProcessDefinitionKey()).isEqualTo(FILE_MODEL_DEFINITION_KEY);
        assertThat(fileProcessInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);

        // Проверка передачи переменных файлу
        List<VariableInstance> variables = processRuntime.variables(ProcessPayloadBuilder
                .variables()
                .withProcessInstance(fileProcessInstance)
                .build()
        );
        for (VariableInstance var : variables) {
            logger.info("> Found File Process Instance Variable: " + var);
            switch (var.getName()) {
                case "name":
                    assertThat(var.getValue().toString()).isEqualTo("My First File");
                    break;
                case "initiator_group":
                    assertThat(var.getValue().toString()).isEqualTo("activitiTeam");
                    break;
            }
        }
    }
}
