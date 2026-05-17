package fteam.testutil;

/** Shared YAML fixtures used by server-side parser, worker, and handler tests. */
public final class PipelineFixtures {

  public static final String VALID_SINGLE_JOB = """
      pipeline:
        name: simple-pipeline
      stages:
        - build
      compile:
        stage: build
        script: echo hello
      """;

  public static final String VALID_WITH_DESCRIPTION = """
      pipeline:
        name: described-pipeline
        description: A pipeline with a description
      stages:
        - build
      compile:
        stage: build
        script: echo hello
      """;

  public static final String VALID_TWO_STAGES = """
      pipeline:
        name: two-stage-pipeline
      stages:
        - build
        - test
      compile:
        stage: build
        script: echo compile
      run-tests:
        stage: test
        script: echo test
      """;

  public static final String VALID_WITH_NEEDS = """
      pipeline:
        name: needs-pipeline
      stages:
        - test
      unit-test:
        stage: test
        script: echo unit
      integration-test:
        stage: test
        needs:
          - unit-test
        script: echo integration
      """;

  public static final String VALID_WITH_ALLOWED_FAILURE = """
      pipeline:
        name: failure-pipeline
      stages:
        - test
      flakey-job:
        stage: test
        script: exit 1
        failures: true
      """;

  public static final String INVALID_MISSING_PIPELINE_SECTION = """
      stages:
        - build
      compile:
        stage: build
        script: echo hello
      """;

  public static final String INVALID_MISSING_STAGES = """
      pipeline:
        name: no-stages
      compile:
        stage: build
        script: echo hello
      """;

  public static final String INVALID_EMPTY_STAGES = """
      pipeline:
        name: empty-stages
      stages: []
      compile:
        stage: build
        script: echo hello
      """;

  public static final String INVALID_UNDEFINED_STAGE = """
      pipeline:
        name: bad-stage
      stages:
        - build
      compile:
        stage: nonexistent
        script: echo hello
      """;

  public static final String INVALID_CYCLE = """
      pipeline:
        name: cyclic
      stages:
        - build
      job-a:
        stage: build
        needs:
          - job-b
        script: echo a
      job-b:
        stage: build
        needs:
          - job-a
        script: echo b
      """;

  public static final String INVALID_UNKNOWN_NEED = """
      pipeline:
        name: bad-need
      stages:
        - test
      unit-test:
        stage: test
        needs:
          - nonexistent-job
        script: echo test
      """;

  public static final String INVALID_EMPTY_YAML = "";

  private PipelineFixtures() {}
}
