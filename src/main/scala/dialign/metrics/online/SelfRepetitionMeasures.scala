package dialign.metrics.online

import dialign.DialogueLexicon

case class SelfRepetitionMeasures(lexicon: DialogueLexicon) {
  private val lastTurnID = lexicon.turns.size - 1
  private val lastTurn = lexicon.turns(lastTurnID)

  import lexicon._

  /**
    * The self-expression repetition measure of the last turn of the dialogue, i.e.
    * a measure of the self-repetition lexicon usage
    *
    */
  val selfExpressionRepetition =
    if (lexicon.turns(lastTurnID).tokenSize > 0) {
      lastTurn.rawExprsTokenSize.toDouble / lexicon.turns(lastTurnID).tokenSize.toDouble
    } else {
      0.0d
    }

  /**
    * Dialogue structures considered in the computation of the activation
    */
  val dialogueStructures = lastTurn.allExpressions.filter(_.turns().size > 0)
  /**
    * Number of considered dialogue structures
    */
  val N = dialogueStructures.size
}
