package sg.edu.ntu.taskflowapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Task is a POJO - a "Plain Old Java Object".
 *
 * That means it is just data: fields, getters and setters, and nothing else.
 * It has NO Spring annotations (@Component / @Service / @Repository) because
 * it is not a bean. Spring never needs to create or manage a Task - our own
 * code creates Tasks, and Spring only converts them to and from JSON.
 *
 * Lombok generates the boilerplate for us at compile time:
 *   @Getter            -> getId(), getTitle(), isCompleted()
 *   @Setter            -> setId(...), setTitle(...), setCompleted(...)
 *   @NoArgsConstructor -> new Task()            <- Jackson needs this to build
 *                                                  a Task from incoming JSON
 *   @AllArgsConstructor-> new Task(id, title, completed)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    // The repository generates this value - a client is never trusted to set it.
    private Long id;

    private String title;

    // A primitive boolean defaults to false, so a new task starts "not done".
    // Lombok names the getter isCompleted() for booleans, not getCompleted().
    private boolean completed;
}
