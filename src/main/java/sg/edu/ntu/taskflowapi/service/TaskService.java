package sg.edu.ntu.taskflowapi.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;

/**
 * The SERVICE layer - where the business logic lives.
 *
 * @Service tells Spring to manage this class as a bean. Technically it behaves
 * the same as @Component, but it communicates the class's role: this is the
 * layer that makes decisions.
 *
 * Why does the service sit between the controller and the repository?
 *   - the controller should only speak HTTP (statuses, request bodies, URLs)
 *   - the repository should only speak storage (put, get, remove)
 *   - the rules in between - "keep the URL id", "flip completed to true" -
 *     belong here, so both of the other layers stay thin and simple.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ChatClient chatClient;

    /**
     * CONSTRUCTOR INJECTION.
     *
     * We do not create the dependencies ourselves with `new`, and we do not
     * use @Autowired on a field. Instead we declare what this class needs as
     * constructor parameters, and Spring supplies the matching beans when it
     * builds TaskService at startup. The fields are `final`, which proves the
     * dependencies are set exactly once and never reassigned.
     *
     * ChatClient.Builder is a factory bean that the Spring AI starter creates
     * for us, already configured from application.properties (API key, model,
     * temperature). We call .build() once, here, to turn that builder into the
     * ready-to-use ChatClient we will send prompts with.
     */
    public TaskService(TaskRepository taskRepository, ChatClient.Builder chatClientBuilder) {
        this.taskRepository = taskRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    // Returns null when nothing is stored under this id - the controller
    // translates that null into 404 Not Found.
    public Task findTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task createTask(Task task) {
        // The repository generates the id, so any id the client put in the
        // request body is simply overwritten there and ignored.
        return taskRepository.save(task);
    }

    /**
     * UPDATE.
     *
     * The id in the URL is the authoritative one. The request body only carries
     * the new field VALUES - if it happens to contain an "id" we must not let
     * it move or duplicate the stored task. So we:
     *   1. look the existing task up by the URL id (null -> nothing to update)
     *   2. copy the new values onto the task we already have
     *   3. hand it back to the repository to be stored under the same key
     */
    public Task updateTask(Long id, Task task) {
        Task existingTask = taskRepository.findById(id);
        if (existingTask == null) {
            return null;
        }

        existingTask.setTitle(task.getTitle());
        existingTask.setCompleted(task.isCompleted());

        return taskRepository.update(id, existingTask);
    }

    // true = a task was deleted, false = there was nothing with that id.
    public boolean deleteTask(Long id) {
        return taskRepository.deleteById(id);
    }

    /**
     * MARK AS COMPLETE.
     *
     * A small piece of business logic: find the task, flip its completed flag,
     * save it, and return it. Calling this on a task that is already complete
     * is harmless - it just stays true.
     */
    public Task markTaskAsComplete(Long id) {
        Task task = taskRepository.findById(id);
        if (task == null) {
            return null;
        }

        task.setCompleted(true);

        return taskRepository.update(id, task);
    }

    /**
     * SPRING AI SUMMARY.
     *
     * Builds a plain-text description of the current task list and asks the
     * model to turn it into a short human-readable summary. The summary is
     * never hardcoded - it is generated from whatever is in the HashMap now.
     */
    public String summariseTasks() {
        List<Task> allTasks = findAllTasks();

        if (allTasks.isEmpty()) {
            // No point paying for an AI call when there is nothing to summarise.
            return "You have no tasks yet.";
        }

        // Turn the task objects into simple lines the model can read, e.g.
        //   - "Study Spring Boot" (pending)
        StringBuilder taskList = new StringBuilder();
        for (Task task : allTasks) {
            taskList.append("- \"")
                    .append(task.getTitle())
                    .append("\" (")
                    .append(task.isCompleted() ? "completed" : "pending")
                    .append(")\n");
        }

        // The ChatClient call, exactly as taught in the Spring AI lessons:
        //   .prompt()  - start building a request
        //   .user(...) - the message we are sending to the model
        //   .call()    - send it and wait for the reply
        //   .content() - pull the reply out as a plain String
        return chatClient.prompt()
                .user("Here is a task list:\n" + taskList
                        + "\nWrite a short plain-English summary in 2-3 sentences. "
                        + "Say how many tasks there are in total, how many are completed, "
                        + "how many are still pending, and name the pending ones.")
                .call()
                .content();
    }
}
