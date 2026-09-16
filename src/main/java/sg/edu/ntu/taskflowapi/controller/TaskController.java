package sg.edu.ntu.taskflowapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.service.TaskService;

/**
 * The CONTROLLER layer - the front door of the application.
 *
 * @RestController = @Controller + @ResponseBody. It marks this class as a
 * Spring bean that handles web requests, and it tells Spring to convert every
 * returned object straight into JSON instead of looking for an HTML page.
 *
 * @RequestMapping("/api/tasks") sets the base URL for the whole class, so the
 * methods below only need to declare what comes AFTER it. @GetMapping("/{id}")
 * therefore means GET /api/tasks/{id}.
 *
 * The controller stays THIN: read the request, call the service, choose the
 * HTTP status. No storage logic and no business rules live here.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // Constructor injection again: Spring passes in the TaskService bean it
    // built at startup. The controller never calls `new TaskService()`.
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * ResponseEntity lets us control the whole HTTP response - the body AND
     * the status code. Without it we could only return the body and would
     * always get a 200, which makes 201 Created and 404 Not Found impossible.
     */

    // GET /api/tasks -> 200 OK
    @GetMapping("")
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> allTasks = taskService.findAllTasks();
        // An empty list is still a successful answer, so this is 200 with [].
        return new ResponseEntity<>(allTasks, HttpStatus.OK);
    }

    /**
     * GET /api/tasks/summary -> 200 OK
     *
     * This must be declared as a literal path. Spring always prefers an exact
     * text match over a {placeholder}, so "/summary" is never mistaken for an
     * id - but writing it out plainly also makes that obvious to a reader.
     */
    @GetMapping("/summary")
    public ResponseEntity<String> getTaskSummary() {
        String summary = taskService.summariseTasks();
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    // GET /api/tasks/{id} -> 200 OK, or 404 if there is no such task.
    // @PathVariable pulls the {id} out of the URL and hands it to us as a Long.
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Task foundTask = taskService.findTaskById(id);
        if (foundTask == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(foundTask, HttpStatus.OK);
    }

    /**
     * POST /api/tasks -> 201 Created
     *
     * @RequestBody tells Spring to read the JSON in the request body and turn
     * it into a Task object for us (this is why Task needs a no-arg constructor
     * and setters).
     *
     * 201 Created - not 200 - because a NEW resource was added. The response
     * body contains the created task including the id the repository generated.
     */
    @PostMapping("")
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task newTask = taskService.createTask(task);
        return new ResponseEntity<>(newTask, HttpStatus.CREATED);
    }

    // PUT /api/tasks/{id} -> 200 OK, or 404 if there is no such task.
    // The id comes from the URL; the body only supplies the new values.
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        Task updatedTask = taskService.updateTask(id, task);
        if (updatedTask == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }

    // PUT /api/tasks/{id}/complete -> 200 OK, or 404 if there is no such task.
    @PutMapping("/{id}/complete")
    public ResponseEntity<Task> markTaskAsComplete(@PathVariable Long id) {
        Task completedTask = taskService.markTaskAsComplete(id);
        if (completedTask == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(completedTask, HttpStatus.OK);
    }

    // DELETE /api/tasks/{id} -> 200 OK, or 404 if there was nothing to delete.
    // The assignment asks for 200 (with a short confirmation message) rather
    // than the 204 No Content you may see in other REST examples.
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id) {
        boolean deleted = taskService.deleteTask(id);
        if (!deleted) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("Task " + id + " deleted.", HttpStatus.OK);
    }
}
