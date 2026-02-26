package fteam.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.*;

public class PipelineConfig {
  private Set<String> stages;
  private String name;
  private String description;
  private List<Job> jobs;
  private List<Job> excutionSequence=new ArrayList<>();
  private List<String> messages=new ArrayList<>();
  private boolean isValid;
  private File currentFile;
  private Map<String, Mark> needsMarkByJob = new HashMap<>();
  private List<String> stagesInOrder = new ArrayList<>();
  private Map<String, Mark> stageMarkByJob = new HashMap<>();
  private Map<String, Map<String, Mark>> needsItemMarkByJob = new HashMap<>();
  private enum State {
    VISITING, VISITED
  }

  private PipelineConfig(Set<String> stages, List<String> stagesInOrder, String name, String description, List<Job> jobs){
    this.stages = stages;
    this.stagesInOrder = stagesInOrder;
    this.name = name;
    this.description = description;
    this.jobs = jobs;
  }

  private void verifyAndBuild(){
    isValid = true;
    verifyStages();
    verifyJobs();
    verifyEmptyStages();
    buildJobGraph();

    if (!messages.isEmpty()) {
      isValid = false;
      excutionSequence.clear();
    }
  }

  private void verifyJobs() {
    Set<String> jobNames = new HashSet<>();
    for (Job job : jobs) {
      if (!jobNames.add(job.getName())) {
        messages.add("duplicate job name: " + job.getName());
      }

      if (!stages.contains(job.getStage())) {
        Mark m = stageMarkByJob.get(job.getName());
        messages.add(line(m) + ":" + col(m) +
            ": job " + job.getName() +
            " uses undefined stage " + job.getStage());
      }
    }

  }
  private void verifyEmptyStages() {
    Map<String, Integer> counts = new HashMap<>();
    for (String s : stages) counts.put(s, 0);

    for (Job j : jobs) {
      counts.put(j.getStage(),
          counts.getOrDefault(j.getStage(), 0) + 1);
    }

    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      if (e.getValue() == 0) {
        messages.add("stage `" + e.getKey() + "` has no jobs");
      }
    }
  }

  private void verifyStages() {
    if (stages == null || stages.isEmpty()) {
      messages.add("no stages defined");
    }
  }
  private static MappingNode asMapping(Node n) {
    return (n instanceof MappingNode) ? (MappingNode) n : null;
  }

  private static SequenceNode asSequence(Node n) {
    return (n instanceof SequenceNode) ? (SequenceNode) n : null;
  }

  private static Node findValueNode(MappingNode map, String key) {
    for (NodeTuple t : map.getValue()) {
      Node k = t.getKeyNode();
      if (k instanceof ScalarNode s && key.equals(s.getValue())) {
        return t.getValueNode();
      }
    }
    return null;
  }

  private static Mark findKeyMark(MappingNode map, String key) {
    for (NodeTuple t : map.getValue()) {
      Node k = t.getKeyNode();
      if (k instanceof ScalarNode s && key.equals(s.getValue())) {
        return k.getStartMark();
      }
    }
    return null;
  }

  private static int line(Mark m) { return (m == null) ? 1 : m.getLine() + 1; }
  private static int col(Mark m)  { return (m == null) ? 1 : m.getColumn() + 1; }

  private void buildJobGraph() {
    excutionSequence.clear();
    Map<String, Map<String, Job>> stageJobMap = new HashMap<>();
    for (Job job : jobs) {
      stageJobMap.computeIfAbsent(job.getStage(), k -> new HashMap<>())
          .put(job.getName(), job);
    }

    for (String stage : stagesInOrder) {
      Map<String, Job> jobMap = stageJobMap.getOrDefault(stage, Map.of());

      if (jobMap.isEmpty()) continue;

      for (Job job : jobMap.values()) {
        for (String dep : job.getNeeds()) {
          if (!jobMap.containsKey(dep)) {
            Mark m = null;
            Map<String, Mark> depMarks = needsItemMarkByJob.get(job.getName());
            if (depMarks != null) m = depMarks.get(dep);
            if (m == null) m = needsMarkByJob.get(job.getName());
            messages.add(line(m) + ":" + col(m)
                + ": job " + job.getName()
                + " needs unknown job `" + dep + "` in stage " + stage);
          }
        }
      }
      if (!messages.isEmpty()) return;

      // topo sort within stage
      Map<String, State> state = new HashMap<>();
      List<Job> stageOrder = new ArrayList<>();

      List<String> names = new ArrayList<>(jobMap.keySet());
      names.sort(String::compareTo);

      for (String jobName : names) {
        if (!state.containsKey(jobName)) {
          topoDfs(jobName, jobMap, state, new ArrayList<>(), stageOrder);
          if (!messages.isEmpty()) return;
        }
      }

      excutionSequence.addAll(stageOrder);
    }
  }


  private void topoDfs(
      String jobName,
      Map<String, Job> jobMap,
      Map<String, State> state,
      List<String> path,
      List<Job> out
  ) {
    State st = state.get(jobName);
    if (st == State.VISITING) {
      String cycle = String.join(" -> ", path) + " -> " + jobName;
      Mark m = needsMarkByJob.get(jobName);
      messages.add(line(m) + ":" + col(m) + ": cycle detected in `needs`: " + cycle);
      return;
    }
    if (st == State.VISITED) return;

    state.put(jobName, State.VISITING);
    path.add(jobName);

    Job cur = jobMap.get(jobName);
    if (cur == null) {
      messages.add(currentFile.getPath() + ":1:1: internal: unknown job referenced: `" + jobName + "`");
      return;
    }

    for (String dep : cur.getNeeds()) {
      topoDfs(dep, jobMap, state, path, out);
      if (!messages.isEmpty()) return;
    }

    state.put(jobName, State.VISITED);
    path.remove(path.size() - 1);
    out.add(cur);
  }



  private static PipelineConfig badAt(File file, int line, int col, String msg) {
    PipelineConfig pc =
        new PipelineConfig(new HashSet<>(), new ArrayList<>(), null, null, new ArrayList<>());
    pc.isValid = false;
    pc.messages.add(line + ":" + col + ": " + msg);
    return pc;
  }

  private static PipelineConfig bad(File file, String msg) {
    return badAt(file, 1, 1, msg);
  }


  public boolean isvalidConfig(){
    return isValid;
  }

  public List<String> getVerificationMsg(){
    return messages;
  }
  public static PipelineConfig fromFile(String pathOrYaml) {
    if (pathOrYaml.contains("\n") ||
        pathOrYaml.contains("pipeline:") ||
        pathOrYaml.contains("stages:")) {
      return fromYamlString(pathOrYaml);
    }
    return fromFileContent(new File(pathOrYaml));
  }
  public static PipelineConfig fromYamlString(String yamlText) {
    return fromText(yamlText, new File("<inline-yaml>"));
  }

  private static PipelineConfig fromText(String text, File sourceFileForMsg) {
    Yaml yaml = new Yaml();

    final Node rootNode;
    final Object loaded;
    try {
      rootNode = yaml.compose(new java.io.StringReader(text));
      loaded = yaml.load(new java.io.StringReader(text));
    } catch (Exception e) {
      return badAt(sourceFileForMsg, 1, 1, "invalid YAML: " + e.getMessage());
    }

    if (rootNode == null || loaded == null) {
      return badAt(sourceFileForMsg, 1, 1, "empty YAML file (no content)");
    }

    final MappingNode rootMapNode = asMapping(rootNode);
    if (rootMapNode == null) {
      return badAt(sourceFileForMsg, line(rootNode.getStartMark()), col(rootNode.getStartMark()),
          "top-level YAML must be a mapping (key-value pairs)");
    }

    final Map<String, Object> root = asMap(loaded);
    if (root == null) {
      return badAt(sourceFileForMsg, line(rootNode.getStartMark()), col(rootNode.getStartMark()),
          "top-level YAML must be a mapping (key-value pairs)");
    }

    // ---------- pipeline (node) ----------
    Node pipelineNode = findValueNode(rootMapNode, "pipeline");
    if (pipelineNode == null) {
      Mark m = rootNode.getStartMark();
      return badAt(sourceFileForMsg, line(m), col(m), "missing required `pipeline` section");
    }

    MappingNode pipelineMapNode = asMapping(pipelineNode);
    if (pipelineMapNode == null) {
      Mark km = findKeyMark(rootMapNode, "pipeline");
      return badAt(sourceFileForMsg, line(km), col(km), "`pipeline` must be a mapping");
    }

    // ---------- stages (node) ----------
    Node stagesNode = findValueNode(rootMapNode, "stages");
    if (stagesNode == null) {
      return badAt(sourceFileForMsg, 1, 1, "missing required `stages` (expected list)");
    }
    if (asSequence(stagesNode) == null) {
      Mark kmStages = findKeyMark(rootMapNode, "stages");
      return badAt(sourceFileForMsg, line(kmStages), col(kmStages), "`stages` must be a list");
    }

    // ---------- pipeline values ----------
    Map<String, Object> pipeline = asMap(root.get("pipeline"));
    if (pipeline == null) {
      Mark km = findKeyMark(rootMapNode, "pipeline");
      return badAt(sourceFileForMsg, line(km), col(km), "`pipeline` must be a mapping");
    }

    Mark kmName = findKeyMark(pipelineMapNode, "name");
    Object nameObj = pipeline.get("name");

    if (nameObj == null) {
      return badAt(sourceFileForMsg, line(kmName), col(kmName), "missing required `pipeline.name`");
    }
    if (!(nameObj instanceof String)) {
      return badAt(sourceFileForMsg, line(kmName), col(kmName),
          "wrong type of value given for `pipeline.name`. Expected String, given " + nameObj);
    }
    String name = (String) nameObj;
    if (name.isBlank()) {
      return badAt(sourceFileForMsg, line(kmName), col(kmName),
          "`pipeline.name` must be non-empty String");
    }

    // pipeline.description
    Node descNode = findValueNode(pipelineMapNode, "description");
    String description = null;
    if (descNode != null) {
      Mark kmDesc = findKeyMark(pipelineMapNode, "description");
      if (!(descNode instanceof ScalarNode dsn)) {
        return badAt(sourceFileForMsg, line(kmDesc), col(kmDesc),
            "wrong type of value given for `pipeline.description`. Expected String, given " + descNode);
      }
      description = dsn.getValue();
    }

    // ---------- stages values ----------
    Object stagesObj = root.get("stages");
    List<Object> stagesRaw = asList(stagesObj);
    if (stagesRaw == null || stagesRaw.isEmpty()) {
      Mark kmStages = findKeyMark(rootMapNode, "stages");
      return badAt(sourceFileForMsg, line(kmStages), col(kmStages),
          "`stages` must be a non-empty list of Strings");
    }

    List<String> stagesList = new ArrayList<>();
    Set<String> stagesSet = new HashSet<>();
    for (Object s : stagesRaw) {
      if (!(s instanceof String) || ((String) s).isBlank()) {
        Mark kmStages = findKeyMark(rootMapNode, "stages");
        return badAt(sourceFileForMsg, line(kmStages), col(kmStages),
            "wrong type/value in `stages`. Expected non-empty String, given " + s);
      }
      String ss = (String) s;
      if (!stagesSet.add(ss)) {
        Mark kmStages = findKeyMark(rootMapNode, "stages");
        return badAt(sourceFileForMsg, line(kmStages), col(kmStages), "duplicate stage name: " + ss);
      }
      stagesList.add(ss);
    }

    // ---------- jobs (map) ----------
    List<Job> jobs = new ArrayList<>();
    for (Map.Entry<String, Object> entry : root.entrySet()) {
      String key = entry.getKey();
      if (key.equals("pipeline") || key.equals("stages")) continue;

      if (!(entry.getValue() instanceof Map)) {
        Mark kmJob = findKeyMark(rootMapNode, key);
        return badAt(sourceFileForMsg, line(kmJob), col(kmJob),
            "job `" + key + "` must be a mapping with keys: stage/image/script/needs");
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> jobMap = (Map<String, Object>) entry.getValue();

      try {
        jobs.add(Job.fromYaml(key, jobMap));
      } catch (IllegalArgumentException ex) {
        Mark kmJob = findKeyMark(rootMapNode, key);
        return badAt(sourceFileForMsg, line(kmJob), col(kmJob),
            "job `" + key + "`: " + ex.getMessage());
      }
    }

    // ---------- build config + needs mark ----------
    PipelineConfig config = new PipelineConfig(stagesSet, stagesList, name, description, jobs);
    config.currentFile = sourceFileForMsg;

    for (NodeTuple top : rootMapNode.getValue()) {
      Node k = top.getKeyNode();
      if (!(k instanceof ScalarNode ks)) continue;

      String topKey = ks.getValue();
      if (topKey.equals("pipeline") || topKey.equals("stages")) continue;

      MappingNode jobNode = asMapping(top.getValueNode());
      if (jobNode == null) continue;

      Node needsValueNode = findValueNode(jobNode, "needs");
      if (needsValueNode instanceof SequenceNode seq) {
        Map<String, Mark> depMarks = new HashMap<>();
        for (Node item : seq.getValue()) {
          if (item instanceof ScalarNode sn) {
            String depName = sn.getValue();
            depMarks.put(depName, sn.getStartMark());
          }
        }
        if (!depMarks.isEmpty()) {
          config.needsItemMarkByJob.put(topKey, depMarks);
        }
      }

      Mark needsMark = findKeyMark(jobNode, "needs");
      if (needsMark != null) {
        config.needsMarkByJob.put(topKey, needsMark);
      }
      Mark stageMark = findKeyMark(jobNode, "stage");
      if (stageMark != null) {
        config.stageMarkByJob.put(topKey, stageMark);
      }

    }

    config.verifyAndBuild();
    return config;
  }

  public static PipelineConfig fromFileContent(File file) {
    final String text;
    try {
      text = java.nio.file.Files.readString(file.toPath());
    } catch (Exception e) {
      return badAt(file, 1, 1, "cannot read file: " + e.getMessage());
    }
    return fromText(text, file);
  }




  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) { return (o instanceof Map) ? (Map<String, Object>) o : null; }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object o) { return (o instanceof List) ? (List<Object>) o : null; }

  public Set<String> getStages() {
    return stages;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<Job> getJobs() {
    return jobs;
  }

  public List<Job> getExcutionSequence() {
    return excutionSequence;
  }
  public List<String> getStagesInOrder() { return stagesInOrder; }

}