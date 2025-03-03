package org.oppia.android.domain.state

import org.oppia.android.app.model.AnswerOutcome
import org.oppia.android.app.model.Outcome
import org.oppia.android.app.model.State

/**
 * Graph that provides lookup access for [State]s and functionality for processing the outcome of a submitted learner
 * answer.
 */
internal class StateGraph internal constructor(
  private var stateGraph: Map<String, State>
) {
  /** Resets this graph to the new graph represented by the specified [Map]. */
  internal fun reset(stateGraph: Map<String, State>) {
    this.stateGraph = stateGraph
  }

  /** Returns the [State] corresponding to the specified name. */
  internal fun getState(stateName: String): State {
    return stateGraph.getValue(stateName)
  }

  /** Returns an [AnswerOutcome] based on the current state and resulting [Outcome] from the learner's answer. */
  internal fun computeAnswerOutcomeForResult(currentState: State, outcome: Outcome): AnswerOutcome {
    val answerOutcomeBuilder = AnswerOutcome.newBuilder()
      .setFeedback(outcome.feedback)
      .setLabelledAsCorrectAnswer(outcome.labelledAsCorrect)
      .setState(currentState)
    when {
      outcome.refresherExplorationId.isNotEmpty() ->
        answerOutcomeBuilder.refresherExplorationId = outcome.refresherExplorationId
      outcome.missingPrerequisiteSkillId.isNotEmpty() ->
        answerOutcomeBuilder.missingPrerequisiteSkillId = outcome.missingPrerequisiteSkillId
      outcome.destStateName == currentState.name -> answerOutcomeBuilder.sameState = true
      else -> answerOutcomeBuilder.stateName = outcome.destStateName
    }
    return answerOutcomeBuilder.build()
  }
}
