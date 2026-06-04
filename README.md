# Squirrel

Squirrel is a backup management and execution system built with Scala 3, ZIO, and React.

## Getting Started

To run the project with all its dependencies (PostgreSQL, Minio, Jaeger, etc.), you can use Docker Compose.

1. Create a `.env` file from the example:
   ```bash
   cp .env.example .env
   ```

2. Start the services using Docker Compose:
   ```bash
   docker compose up -d
   ```
   The backend API will be available at `http://localhost:8080` and the frontend at `http://localhost:80`.

## API Endpoints Example

Below is an example of how to create a backup job for the `questify-db` test database and trigger its execution manually using `curl`.

### 1. Create a Backup Job

This command creates a new backup job targeting the `questify-db` Postgres database that runs inside the Docker Compose network.

```bash
curl -X POST http://localhost:8080/backup-jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "questify-test-backup",
    "sourceType": "PostgreSQL",
    "source": "postgres://questify-db@questify-db:5432/questify-db",
    "targetBucket": "squirrelvault",
    "targetPrefix": "questify/postgres",
    "schedule": "0 2 * * *",
    "retentionDays": 14,
    "critical": true,
    "expectedFrequencyHours": 24,
    "enabled": true
  }'
```

The API will return the created job, including its unique `"id"`.

### 2. Execute the Backup Job

To trigger the backup job immediately, use the ID returned from the previous step:

```bash
# Replace <JOB_ID> with the actual ID returned when creating the job
curl -X POST http://localhost:8080/backup-jobs/<JOB_ID>/run
```

This will return a `"runId"`.

### 3. Check Backup Run Status

You can query the status of the backup run using the `runId`:

```bash
# Replace <RUN_ID> with the actual runId returned when triggering the job
curl -X GET http://localhost:8080/backup-runs/<RUN_ID>
```

You can check Minio at `http://localhost:9001` (login `minio`/`changeme` per `.env.example`) to see the uploaded backup file in the `squirrelvault` bucket.

## Development

This is a normal sbt project. You can compile the backend code with `sbt compile` in the `backend` folder, run tests with `sbt test`, and `sbt console` will start a Scala 3 REPL.
