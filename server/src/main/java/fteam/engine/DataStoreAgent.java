package fteam.engine;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class DataStoreAgent {

  private static final DataStoreAgent INSTANCE = new DataStoreAgent();
  public static DataStoreAgent getInstance() { return INSTANCE; }

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

  public int nextRunNo(String pipeline) {
    Document updated = counters.findOneAndUpdate(
        eq("_id", pipeline),
        inc("seq", 1),
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    );
    return updated.getInteger("seq");
  }

  public void startRun(String pipeline, int runNo, OffsetDateTime start,
      String gitRepo, String gitBranch, String gitHash) {
    Document doc = new Document()
        .append("pipeline", pipeline)
        .append("runNo", runNo)
        .append("status", "running")
        .append("startTime", toDate(start))
        .append("endTime", null)
        .append("gitRepo", nz(gitRepo))
        .append("gitBranch", nz(gitBranch))
        .append("gitHash", nz(gitHash));

    runs.updateOne(
        and(eq("pipeline", pipeline), eq("runNo", runNo)),
        new Document("$setOnInsert", doc),
        new UpdateOptions().upsert(true)
    );
  }

  public void finishRun(String pipeline, int runNo, OffsetDateTime end, String status) {
    runs.updateOne(
        and(eq("pipeline", pipeline), eq("runNo", runNo)),
        combine(set("endTime", toDate(end)), set("status", status))
    );
  }

  public void startStage(String pipeline, int runNo, String stage, OffsetDateTime start) {
    Document doc = new Document()
        .append("pipeline", pipeline)
        .append("runNo", runNo)
        .append("stage", stage)
        .append("status", "running")
        .append("startTime", toDate(start))
        .append("endTime", null);

    stages.updateOne(
        and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage)),
        new Document("$setOnInsert", doc),
        new UpdateOptions().upsert(true)
    );
  }

  public void finishStage(String pipeline, int runNo, String stage, OffsetDateTime end, String status) {
    stages.updateOne(
        and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage)),
        combine(set("endTime", toDate(end)), set("status", status))
    );
  }

  public void upsertJob(String pipeline, int runNo, String stage, String job,
      OffsetDateTime start, OffsetDateTime end, String status, String errorMessage) {
    Document setDoc = new Document()
        .append("pipeline", pipeline)
        .append("runNo", runNo)
        .append("stage", stage)
        .append("job", job)
        .append("status", status)
        .append("startTime", start == null ? null : toDate(start))
        .append("endTime", end == null ? null : toDate(end))
        .append("errorMessage", nz(errorMessage));

    jobs.updateOne(
        and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage), eq("job", job)),
        new Document("$set", setDoc),
        new UpdateOptions().upsert(true)
    );
  }

  // ===== report queries =====
  public List<Document> findRuns(String pipeline) {
    return runs.find(eq("pipeline", pipeline))
        .sort(Sorts.ascending("runNo"))
        .into(new java.util.ArrayList<>());
  }

  public Document findRun(String pipeline, int runNo) {
    return runs.find(and(eq("pipeline", pipeline), eq("runNo", runNo))).first();
  }

  public List<Document> findStages(String pipeline, int runNo) {
    return stages.find(and(eq("pipeline", pipeline), eq("runNo", runNo)))
        .sort(Sorts.ascending("stage"))
        .into(new java.util.ArrayList<>());
  }

  public Document findStage(String pipeline, int runNo, String stage) {
    return stages.find(and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage))).first();
  }

  public List<Document> findJobs(String pipeline, int runNo, String stage) {
    return jobs.find(and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage)))
        .sort(Sorts.ascending("job"))
        .into(new java.util.ArrayList<>());
  }

  public Document findJob(String pipeline, int runNo, String stage, String job) {
    return jobs.find(and(eq("pipeline", pipeline), eq("runNo", runNo), eq("stage", stage), eq("job", job))).first();
  }

  private static Date toDate(OffsetDateTime t) {
    return Date.from(t.toInstant());
  }

  private static String nz(String s) { return s == null ? "" : s; }
}