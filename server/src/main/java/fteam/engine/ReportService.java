package fteam.engine;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.bson.Document;

/** Renders pipeline execution data into YAML-like report responses for the API. */
public class ReportService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_START_TIME = "startTime";
  private static final String FIELD_END_TIME = "endTime";

  /** Creates a stateless report service helper. */
  public ReportService() {}

  /**
   * Builds a report for a pipeline, run, stage, or job depending on the provided filters.
   *
   * @param pipeline pipeline name
   * @param runNo optional run number
   * @param stage optional stage name
   * @param job optional job name
   * @return rendered report text
   */
  public static String renderReport(String pipeline, String runNo, String stage, String job) {
    DataStoreAgent store = DataStoreAgent.getInstance();

    Integer run = (runNo == null || runNo.isBlank()) ? null : Integer.parseInt(runNo);
    String st = (stage == null || stage.isBlank()) ? null : stage;
    // 1) pipeline: all runs
    if (run == null) {
      List<Document> runs = store.findRuns(pipeline);
      return renderPipeline(pipeline, runs);
    }

    // run doc is needed for all below
    Document runDoc = store.findRun(pipeline, run);
    if (runDoc == null) {
      return "ERROR: run not found: pipeline=" + pipeline + " run=" + run;
    }

    // 2) pipeline run: list stages
    if (st == null) {
      List<Document> stageDocs = store.findStages(pipeline, run);
      return renderRun(runDoc, stageDocs);
    }

    // stage doc needed for stage/job views
    Document stageDoc = store.findStage(pipeline, run, st);
    if (stageDoc == null) {
      return "ERROR: stage not found: " + st;
    }

    // 3) stage: list jobs
    String jb = (job == null || job.isBlank()) ? null : job;
    if (jb == null) {
      List<Document> jobDocs = store.findJobs(pipeline, run, st);
      return renderStage(runDoc, stageDoc, jobDocs);
    }

    // 4) job: single job
    Document jobDoc = store.findJob(pipeline, run, st, jb);
    if (jobDoc == null) {
      return "ERROR: job not found: " + jb;
    }

    return renderJob(runDoc, stageDoc, jobDoc);
  }

  // ---------------- render helpers ----------------

  private static String renderPipeline(String pipeline, List<Document> runs) {
    StringBuilder sb = new StringBuilder();
    sb.append("pipeline:\n");
    sb.append("  name: ").append(pipeline).append("\n");
    sb.append("  runs:\n");

    for (Document r : runs) {
      sb.append("      - run-no: ").append(nzi(r.getInteger("runNo"))).append("\n");
      sb.append("        status: ").append(nzs(r.getString(FIELD_STATUS))).append("\n");
      sb.append("        git-repo: ").append(nzs(r.getString("gitRepo"))).append("\n");
      sb.append("        git-branch: ").append(nzs(r.getString("gitBranch"))).append("\n");
      sb.append("        git-hash: ").append(nzs(r.getString("gitHash"))).append("\n");
      sb.append("        start: ").append(fmtDate(r.getDate(FIELD_START_TIME))).append("\n");
      sb.append("        end: ").append(fmtDate(r.getDate(FIELD_END_TIME))).append("\n");
    }
    return sb.toString();
  }

  private static String renderRun(Document runDoc, List<Document> stageDocs) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stages:\n");
    for (Document s : stageDocs) {
      sb.append("     - name: ").append(nzs(s.getString("stage"))).append("\n");
      sb.append("       status: ").append(nzs(s.getString(FIELD_STATUS))).append("\n");
      sb.append("       start: ").append(fmtDate(s.getDate(FIELD_START_TIME))).append("\n");
      sb.append("       end: ").append(fmtDate(s.getDate(FIELD_END_TIME))).append("\n");
    }
    return sb.toString();
  }

  private static String renderStage(Document runDoc, Document stageDoc, List<Document> jobDocs) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stage:\n");
    sb.append("    - name: ").append(nzs(stageDoc.getString("stage"))).append("\n");
    sb.append("      status: ").append(nzs(stageDoc.getString(FIELD_STATUS))).append("\n");
    sb.append("      start: ").append(fmtDate(stageDoc.getDate(FIELD_START_TIME))).append("\n");
    sb.append("      end: ").append(fmtDate(stageDoc.getDate(FIELD_END_TIME))).append("\n");
    sb.append("      jobs:\n");

    for (Document j : jobDocs) {
      sb.append("        - name: ").append(nzs(j.getString("job"))).append("\n");
      sb.append("          status: ").append(nzs(j.getString(FIELD_STATUS))).append("\n");
      sb.append("          failures: ").append(nzb(j.getBoolean("failures"))).append("\n");
      sb.append("          start: ").append(fmtDate(j.getDate(FIELD_START_TIME))).append("\n");
      sb.append("          end: ").append(fmtDate(j.getDate(FIELD_END_TIME))).append("\n");

      String err = nzs(j.getString("errorMessage"));
      if (!err.isBlank()) {
        sb.append("          error-message: ").append(err).append("\n");
      }
    }
    return sb.toString();
  }

  private static String renderJob(Document runDoc, Document stageDoc, Document jobDoc) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stage:\n");
    sb.append("    - name: ").append(nzs(stageDoc.getString("stage"))).append("\n");
    sb.append("      status: ").append(nzs(stageDoc.getString(FIELD_STATUS))).append("\n");
    sb.append("      start: ").append(fmtDate(stageDoc.getDate(FIELD_START_TIME))).append("\n");
    sb.append("      end: ").append(fmtDate(stageDoc.getDate(FIELD_END_TIME))).append("\n");
    sb.append("      job:\n");
    sb.append("        - name: ").append(nzs(jobDoc.getString("job"))).append("\n");
    sb.append("          status: ").append(nzs(jobDoc.getString(FIELD_STATUS))).append("\n");
    sb.append("          failures: ").append(nzb(jobDoc.getBoolean("failures"))).append("\n");
    sb.append("          start: ").append(fmtDate(jobDoc.getDate(FIELD_START_TIME))).append("\n");
    sb.append("          end: ").append(fmtDate(jobDoc.getDate(FIELD_END_TIME))).append("\n");

    String err = nzs(jobDoc.getString("errorMessage"));
    if (!err.isBlank()) {
      sb.append("          error-message: ").append(err).append("\n");
    }

    return sb.toString();
  }

  private static String nzb(Boolean b) {
    return b == null ? "false" : String.valueOf(b);
  }

  private static void headerRun(StringBuilder sb, Document runDoc) {
    sb.append("pipeline:\n");
    sb.append("  name: ").append(nzs(runDoc.getString("pipeline"))).append("\n");
    sb.append("  run-no: ").append(nzi(runDoc.getInteger("runNo"))).append("\n");
    sb.append("  status: ").append(nzs(runDoc.getString(FIELD_STATUS))).append("\n");
    sb.append("  trace-id: ").append(nzs(runDoc.getString("traceId"))).append("\n");
    sb.append("  start: ").append(fmtDate(runDoc.getDate(FIELD_START_TIME))).append("\n");
    sb.append("  end: ").append(fmtDate(runDoc.getDate(FIELD_END_TIME))).append("\n");
  }

  // ---------------- formatting helpers ----------------

  @SuppressWarnings("PMD.ReplaceJavaUtilDate")
  private static String fmtDate(Date d) {
    if (d == null) {
      return "null";
    }
    // 你 demo 输出有 -08:00，这里用系统时区格式化（本机是 America/Los_Angeles 就会是 -08/-07）
    OffsetDateTime odt = d.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    return FMT.format(odt);
  }

  private static String nzs(String s) {
    return s == null ? "" : s;
  }

  private static String nzi(Integer i) {
    return i == null ? "" : String.valueOf(i);
  }
}
