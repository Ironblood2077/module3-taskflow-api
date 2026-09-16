package sg.edu.ntu.taskflowapi.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import sg.edu.ntu.taskflowapi.model.Task;

/**
 * The REPOSITORY layer. Its only job is storing and retrieving data.
 * There is no business logic here - just "put it in / take it out".
 *
 * @Repository tells Spring "create one instance of this class and manage it
 * as a bean". It is a specialised @Component that also marks this class as
 * the data-access layer, so anyone reading the code knows where storage lives.
 */
@Repository
public class TaskRepository {

    /**
     * Our in-memory "database" - no MySQL, no JPA, just a HashMap that lives
     * in RAM. Everything is lost when the application stops.
     *
     *   key   = the task's id  (Long)
     *   value = the Task object itself
     *
     * A HashMap is a good fit because looking a task up by its id is the most
     * common operation, and a map does that in one step - no looping required.
     *
     * It is initialised right here on the field so it can never be null.
     */
    private final Map<Long, Task> tasks = new HashMap<>();

    /**
     * The ID counter. IDs are generated HERE, in the repository, because the
     * repository owns the data store - it is the only place that knows which
     * IDs are already taken. We never trust an id sent by a client in a POST
     * body, otherwise two clients could claim the same id and overwrite
     * each other's task.
     *
     * We pre-increment (++nextId), so the first task gets 1, the second 2, etc.
     */
    private long nextId = 0;

    // Returns every task. We copy the values into a new ArrayList so callers
    // cannot modify our internal map by accident.
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    // Returns the task with this id, or null if the map has no such key.
    // Returning null (rather than throwing) keeps the "not found" case simple:
    // the service passes it along and the controller turns it into a 404.
    public Task findById(Long id) {
        return tasks.get(id);
    }

    // CREATE: generate the next id, stamp it onto the task, then store it.
    public Task save(Task task) {
        Long generatedId = ++nextId;
        task.setId(generatedId);
        tasks.put(generatedId, task);
        return task;
    }

    // UPDATE: overwrite whatever is stored under this id.
    // The caller has already confirmed the task exists and has set the correct
    // id on the object, so here we simply put it back into the map.
    public Task update(Long id, Task task) {
        tasks.put(id, task);
        return task;
    }

    // DELETE: remove() returns the removed Task, or null if the key was absent.
    // Turning that into a boolean makes the "did it exist?" answer obvious.
    public boolean deleteById(Long id) {
        return tasks.remove(id) != null;
    }
}
