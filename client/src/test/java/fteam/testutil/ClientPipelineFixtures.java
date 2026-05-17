package fteam.testutil;

/** Shared client-side pipeline fixtures for CLI tests. */
public final class ClientPipelineFixtures {

  /** Minimal valid YAML pipeline used by client tests. */
  public static final String VALID_SINGLE_JOB = """
      pipeline:
        name: simple-pipeline
      stages:
        - build
      compile:
        stage: build
        script: echo hello
      """;

  /** Valid YAML containing an intra-stage dependency. */
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

  private ClientPipelineFixtures() {}
}
