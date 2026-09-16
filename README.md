# TaskFlow - Task Management API

A simple Spring Boot REST API for managing tasks, built for the NTU SCTP
Module 3 mandatory assignment. Tasks are stored **in memory** (a `HashMap`) -
there is no database, so all data is lost when the application stops.

## Technology

- Java 21
- Spring Boot 4.1.1
- Maven (with the Maven wrapper, `./mvnw`)
- Spring Web
- Lombok
- Spring Boot DevTools
- Spring AI (OpenAI starter, BOM 2.0.0)

## Architecture

The application is split into layers, each with one job:

```text
HTTP Request
     ↓
TaskController      receives the request, returns a ResponseEntity + status code
     ↓
TaskService         the business logic (update rules, mark-complete, AI summary)
     ↓
TaskRepository      stores and retrieves data, generates IDs
     ↓
HashMap<Long, Task> the in-memory data store
```

- **Controller** - speaks HTTP only. Thin: read request → call service → pick status.
- **Service** - makes the decisions. Knows nothing about HTTP.
- **Repository** - stores data. Knows nothing about business rules.
- **Model (`Task`)** - a plain POJO carrying `id`, `title` and `completed`.

Each layer is wired to the next using **constructor injection**.

## Package structure

```text
src/main/java/sg/edu/ntu/taskflowapi/
├── controller/TaskController.java
├── model/Task.java
├── repository/TaskRepository.java
└── service/TaskService.java
```

## Endpoints

| Method | Endpoint                    | Purpose                        | Success |
|--------|-----------------------------|--------------------------------|---------|
| GET    | `/api/tasks`                | Get all tasks                  | 200 OK  |
| GET    | `/api/tasks/{id}`           | Get one task by id             | 200 OK  |
| POST   | `/api/tasks`                | Create a task (id is generated)| 201 Created |
| PUT    | `/api/tasks/{id}`           | Update a task                  | 200 OK  |
| DELETE | `/api/tasks/{id}`           | Delete a task                  | 200 OK  |
| PUT    | `/api/tasks/{id}/complete`  | Mark a task as completed       | 200 OK  |
| GET    | `/api/tasks/summary`        | AI-generated summary of tasks  | 200 OK  |

A request for an id that does not exist returns **404 Not Found** instead of
crashing.

### Example - create a task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Study Spring Boot","completed":false}'
```

```json
{ "id": 1, "title": "Study Spring Boot", "completed": false }
```

The repository generates the `id`. Any `id` sent in the request body is ignored.

## Running

```bash
./mvnw spring-boot:run
```

The API is then available at `http://localhost:8080/api/tasks`.

To run the tests:

```bash
./mvnw test
```

## API key

`/api/tasks/summary` calls OpenAI through Spring AI. The API key is **not**
stored in this project. It is read from an environment variable named:

```text
OPENAI_API_KEY
```

Set it in your shell before starting the application:

```bash
export OPENAI_API_KEY=your-key-here
```

`application.properties` only references the variable (`${OPENAI_API_KEY}`) -
the real key never appears in any file in this repository.

The other six endpoints do not use AI and work without the key being set.
