package fteam.engine;

import org.bson.Document;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class ReportService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  public static String renderReport(String pipeline, String runNo, String stage, String job) {
    DataStoreAgent store = DataStoreAgent.getInstance();

    Integer run = (runNo == null || runNo.isBlank()) ? null : Integer.parseInt(runNo);
    String st = (stage == null || stage.isBlank()) ? null : stage;
    String jb = (job == null || job.isBlank()) ? null : job;

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
      sb.append("        status: ").append(nzs(r.getString("status"))).append("\n");
      sb.append("        git-repo: ").append(nzs(r.getString("gitRepo"))).append("\n");
      sb.append("        git-branch: ").append(nzs(r.getString("gitBranch"))).append("\n");
      sb.append("        git-hash: ").append(nzs(r.getString("gitHash"))).append("\n");
      sb.append("        start: ").append(fmtDate(r.getDate("startTime"))).append("\n");
      sb.append("        end: ").append(fmtDate(r.getDate("endTime"))).append("\n");
    }
    return sb.toString();
  }

  private static String renderRun(Document runDoc, List<Document> stageDocs) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stages:\n");
    for (Document s : stageDocs) {
      sb.append("     - name: ").append(nzs(s.getString("stage"))).append("\n");
      sb.append("       status: ").append(nzs(s.getString("status"))).append("\n");
      sb.append("       start: ").append(fmtDate(s.getDate("startTime"))).append("\n");
      sb.append("       end: ").append(fmtDate(s.getDate("endTime"))).append("\n");
    }
    return sb.toString();
  }

  private static String renderStage(Document runDoc, Document stageDoc, List<Document> jobDocs) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stage:\n");
    sb.append("    - name: ").append(nzs(stageDoc.getString("stage"))).append("\n");
    sb.append("      status: ").append(nzs(stageDoc.getString("status"))).append("\n");
    sb.append("      start: ").append(fmtDate(stageDoc.getDate("startTime"))).append("\n");
    sb.append("      end: ").append(fmtDate(stageDoc.getDate("endTime"))).append("\n");
    sb.append("      jobs:\n");

    for (Document j : jobDocs) {
      sb.append("        - name: ").append(nzs(j.getString("job"))).append("\n");
      sb.append("          status: ").append(nzs(j.getString("status"))).append("\n");
      sb.append("          start: ").append(fmtDate(j.getDate("startTime"))).append("\n");
      sb.append("          end: ").append(fmtDate(j.getDate("endTime"))).append("\n");
    }
    return sb.toString();
  }

  private static String renderJob(Document runDoc, Document stageDoc, Document jobDoc) {
    StringBuilder sb = new StringBuilder();
    headerRun(sb, runDoc);

    sb.append("  stage:\n");
    sb.append("    - name: ").append(nzs(stageDoc.getString("stage"))).append("\n");
    sb.append("      status: ").append(nzs(stageDoc.getString("status"))).append("\n");
    sb.append("      start: ").append(fmtDate(stageDoc.getDate("startTime"))).append("\n");
    sb.append("      end: ").append(fmtDate(stageDoc.getDate("endTime"))).append("\n");
    sb.append("      job:\n");
    sb.append("        - name: ").append(nzs(jobDoc.getString("job"))).append("\n");
    sb.append("          status: ").append(nzs(jobDoc.getString("status"))).append("\n");
    sb.append("          start: ").append(fmtDate(jobDoc.getDate("startTime"))).append("\n");
    sb.append("          end: ").append(fmtDate(jobDoc.getDate("endTime"))).append("\n");

    return sb.toString();
  }

  private static void headerRun(StringBuilder sb, Document runDoc) {
    sb.append("pipeline:\n");
    sb.append("  name: ").append(nzs(runDoc.getString("pipeline"))).append("\n");
    sb.append("  run-no: ").append(nzi(runDoc.getInteger("runNo"))).append("\n");
    sb.append("  status: ").append(nzs(runDoc.getString("status"))).append("\n");
    sb.append("  start: ").append(fmtDate(runDoc.getDate("startTime"))).append("\n");
    sb.append("  end: ").append(fmtDate(runDoc.getDate("endTime"))).append("\n");
  }

  // ---------------- formatting helpers ----------------

  private static String fmtDate(Date d) {
    if (d == null) return "null";
    // 你 demo 输出有 -08:00，这里用系统时区格式化（本机是 America/Los_Angeles 就会是 -08/-07）
    OffsetDateTime odt = d.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    return FMT.format(odt);
  }

  private static String nzs(String s) { return s == null ? "" : s; }
  private static String nzi(Integer i) { return i == null ? "" : String.valueOf(i); }
}