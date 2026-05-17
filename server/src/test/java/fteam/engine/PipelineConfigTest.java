package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.PipelineFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests parsing, validation, and execution ordering for pipeline configs. */
class PipelineConfigTest {

  @Test
  void validPipelineParsesAndBuildsExecutionSequence() {
    PipelineConfig cfg = PipelineConfig.fromYamlString(PipelineFixtures.VALID_TWO_STAGES);
    assertTrue(cfg.isvalidConfig());
    assertEquals("two-stage-pipeline", cfg.getName());
    assertEquals(List.of("build", "test"), cfg.getStagesInOrder());
    assertNotNull(cfg.getJobs());
    assertEquals(2, cfg.getExcutionSequence().size());
  }

  @Test
  void needsOrderingIsRespected() {
    PipelineConfig cfg = PipelineConfig.fromYamlString(PipelineFixtures.VALID_WITH_NEEDS);
    List<Job> seq = cfg.getExcutionSequence();
    assertTrue(cfg.isvalidConfig());
    assertEquals("unit-test", seq.get(0).getName());
    assertEquals("integration-test", seq.get(1).getName());
  }

  @Test
  void allowedFailureFlagIsMapped() {
    PipelineConfig cfg =
        PipelineConfig.fromYamlString(PipelineFixtures.VALID_WITH_ALLOWED_FAILURE);
    assertTrue(cfg.isvalidConfig());
    assertTrue(cfg.getExcutionSequence().get(0).isFailureAllowed());
  }

  @Test
  void missingPipelineSectionAndMissingStagesAreInvalid() {
    assertFalse(
        PipelineConfig.fromYamlString(PipelineFixtures.INVALID_MISSING_PIPELINE_SECTION)
            .isvalidConfig());
    assertFalse(
        PipelineConfig.fromYamlString(PipelineFixtures.INVALID_MISSING_STAGES).isvalidConfig());
  }

  @Test
  void invalidCasesEmitHelpfulMessages() {
    PipelineConfig badStage =
        PipelineConfig.fromYamlString(PipelineFixtures.INVALID_UNDEFINED_STAGE);
    PipelineConfig cycle = PipelineConfig.fromYamlString(PipelineFixtures.INVALID_CYCLE);
    PipelineConfig badNeed =
        PipelineConfig.fromYamlString(PipelineFixtures.INVALID_UNKNOWN_NEED);

    assertTrue(badStage.getVerificationMsg().stream().anyMatch(m -> m.contains("nonexistent")));
    assertTrue(cycle.getVerificationMsg().stream().anyMatch(m -> m.contains("cycle")));
    assertTrue(badNeed.getVerificationMsg().stream().anyMatch(m -> m.contains("nonexistent-job")));
  }
}
