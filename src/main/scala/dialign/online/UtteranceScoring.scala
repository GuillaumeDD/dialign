package dialign.online

import dialign.Expression

/**
  * Scoring results for a specific utterance
  *
  * @param utterance                    the scored utterance
  * @param der                          dynamic shared expression repetition
  * @param dser                         dynamic self-expression repetitions
  * @param sharedExpressions            the shared expressions present in the utterance
  * @param establishedSharedExpressions the established shared expressions by this utterance
  * @param selfExpressions              the self-expressions present in the utterance
  */
case class UtteranceScoring(utterance: Utterance,
                            der: Double,
                            dser: Double,
                            sharedExpressions: Set[Expression],
                            establishedSharedExpressions: Set[Expression],
                            selfExpressions: Set[Expression])
