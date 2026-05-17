package fteam.engine;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import org.bson.Document;

/** MongoDB-backed persistence layer for pipeline, stage, and job execution records. */
public class DataStoreAgent {

  private static final DataStoreAgent INSTANCE = new DataStoreAgent();
  private static final String FIELD_PIPELINE = "pipeline";
  private static final String FIELD_RUN_NO = "runNo";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_END_TIME = "endTime";
  private static final String FIELD_STAGE = "stage";
  private static final String FIELD_JOB = "job";

  /**
   * Returns the singleton datastore instance.
   *
   * @return shared datastore instance
   */
  public static DataStoreAgent getInstance() {
    return INSTANCE;
  }

  private final MongoClient client;
  private final MongoDatabase db;

  private final MongoCollection<Document> runs;
  private final MongoCollection<Document> stages;
  private final MongoCollection<Document> jobs;
  private final MongoCollection<Document> counters;

  private DataStoreAgent() {

    String uri = System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017");
    String dbName = System.getenv().getOrDefault("MONGO_DB", "cicd");

    this.client = MongoClients.create(uri);
    this.db = client.getDatabase(dbName);

    this.runs = db.getCollection("pipeline_runs");
    this.stages = db.getCollection("stage_runs");
    this.jobs = db.getCollection("job_runs");
    this.counters = db.getCollection("counters");
  }

  /**
   * Allocates the next run number for the given pipeline.
   *
   * @param pipeline pipeline name
   * @return next sequential run number
   */
  public int nextRunNo(String pipeline) {
    Document updated =
        counters.findOneAndUpdate(
            eq("_id", pipeline),
            inc("seq", 1),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
    return updated.getInteger("seq");
  }

  /**
   * Records the start of a pipeline run.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param start run start time
   * @param gitRepo repository identifier
   * @param gitBranch branch used for the run
   * @param gitHash commit hash used for the run
   */
  public void startRun(
      String pipeline,
      int runNo,
      OffsetDateTime start,
      String gitRepo,
      String gitBranch,
      String gitHash) {
    Document doc =
        new Document()
            .append(FIELD_PIPELINE, pipeline)
            .append(FIELD_RUN_NO, runNo)
            .append(FIELD_STATUS, "running")
            .append("startTime", toDate(start))
            .append(FIELD_END_TIME, null)
            .append("gitRepo", nz(gitRepo))
            .append("gitBranch", nz(gitBranch))
            .append("gitHash", nz(gitHash));

    runs.updateOne(
        and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo)),
        new Document("$setOnInsert", doc),
        new UpdateOptions().upsert(true));
  }

  /**
   * Records the completion of a pipeline run.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param end run end time
   * @param status final run status
   */
  public void finishRun(String pipeline, int runNo, OffsetDateTime end, String status) {
    runs.updateOne(
        and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo)),
        combine(set(FIELD_END_TIME, toDate(end)), set(FIELD_STATUS, status)));
  }

  /**
   * Stores the trace identifier associated with a pipeline run.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param traceId trace identifier
   */
  public void setTraceId(String pipeline, int runNo, String traceId) {
    runs.updateOne(
        and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo)), set("traceId", traceId));
  }

  /**
   * Records the start of a stage execution.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @param start stage start time
   */
  public void startStage(String pipeline, int runNo, String stage, OffsetDateTime start) {
    Document doc =
        new Document()
            .append(FIELD_PIPELINE, pipeline)
            .append(FIELD_RUN_NO, runNo)
            .append(FIELD_STAGE, stage)
            .append(FIELD_STATUS, "running")
            .append("startTime", toDate(start))
            .append(FIELD_END_TIME, null);

    stages.updateOne(
        and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo), eq(FIELD_STAGE, stage)),
        new Document("$setOnInsert", doc),
        new UpdateOptions().upsert(true));
  }

  /**
   * Records the completion of a stage execution.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @param end stage end time
   * @param status final stage status
   */
  public void finishStage(
      String pipeline, int runNo, String stage, OffsetDateTime end, String status) {
    stages.updateOne(
        and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo), eq(FIELD_STAGE, stage)),
        combine(set(FIELD_END_TIME, toDate(end)), set(FIELD_STATUS, status)));
  }

  /**
   * Inserts or updates the execution record for a single job.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @param job job name
   * @param start job start time
   * @param end job end time
   * @param status final job status
   * @param errorMessage recorded error message, if any
   * @param failures whether the job is allowed to fail
   */
  public void upsertJob(
      String pipeline,
      int runNo,
      String stage,
      String job,
      OffsetDateTime start,
      OffsetDateTime end,
      String status,
      String errorMessage,
      boolean failures) {
    Document setDoc =
        new Document()
            .append(FIELD_PIPELINE, pipeline)
            .append(FIELD_RUN_NO, runNo)
            .append(FIELD_STAGE, stage)
            .append(FIELD_JOB, job)
            .append(FIELD_STATUS, status)
            .append("startTime", start == null ? null : toDate(start))
            .append(FIELD_END_TIME, end == null ? null : toDate(end))
            .append("errorMessage", nz(errorMessage))
            .append("failures", failures);

    jobs.updateOne(
        and(
            eq(FIELD_PIPELINE, pipeline),
            eq(FIELD_RUN_NO, runNo),
            eq(FIELD_STAGE, stage),
            eq(FIELD_JOB, job)),
        new Document("$set", setDoc),
        new UpdateOptions().upsert(true));
  }

  // ===== report queries =====
  /**
   * Returns all recorded runs for a pipeline in ascending run order.
   *
   * @param pipeline pipeline name
   * @return list of run documents
   */
  public List<Document> findRuns(String pipeline) {
    return runs.find(eq(FIELD_PIPELINE, pipeline))
        .sort(Sorts.ascending(FIELD_RUN_NO))
        .into(new java.util.ArrayList<>());
  }

  /**
   * Returns a single run document.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @return run document or {@code null} when not found
   */
  public Document findRun(String pipeline, int runNo) {
    return runs.find(and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo))).first();
  }

  /**
   * Returns all recorded stages for a pipeline run.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @return list of stage documents
   */
  public List<Document> findStages(String pipeline, int runNo) {
    return stages
        .find(and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo)))
        .sort(Sorts.ascending(FIELD_STAGE))
        .into(new java.util.ArrayList<>());
  }

  /**
   * Returns a single stage document.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @return stage document or {@code null} when not found
   */
  public Document findStage(String pipeline, int runNo, String stage) {
    return stages
        .find(and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo), eq(FIELD_STAGE, stage)))
        .first();
  }

  /**
   * Returns all recorded jobs for a stage in a pipeline run.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @return list of job documents
   */
  public List<Document> findJobs(String pipeline, int runNo, String stage) {
    return jobs
        .find(and(eq(FIELD_PIPELINE, pipeline), eq(FIELD_RUN_NO, runNo), eq(FIELD_STAGE, stage)))
        .sort(Sorts.ascending(FIELD_JOB))
        .into(new java.util.ArrayList<>());
  }

  /**
   * Returns a single job document.
   *
   * @param pipeline pipeline name
   * @param runNo run number
   * @param stage stage name
   * @param job job name
   * @return job document or {@code null} when not found
   */
  public Document findJob(String pipeline, int runNo, String stage, String job) {
    return jobs.find(
            and(
                eq(FIELD_PIPELINE, pipeline),
                eq(FIELD_RUN_NO, runNo),
                eq(FIELD_STAGE, stage),
                eq(FIELD_JOB, job)))
        .first();
  }

  private static Date toDate(OffsetDateTime t) {
    return Date.from(t.toInstant());
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }
}
