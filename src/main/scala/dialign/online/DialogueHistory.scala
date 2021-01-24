package dialign.online

import dialign.DialogueLexiconBuilder.ExpressionType
import dialign.metrics.online.{LexiconBasedMeasures, SelfRepetitionMeasures}
import dialign.nlp.Tokenizer
import dialign.nlp.Tokenizer.TokenizedUtterance
import dialign.{DialogueLexiconBuilder, Expression, Speaker}

case class Utterance(locutor: String, utterance: String)

sealed abstract class DialogueHistory {

  /**
    *
    * @return the dialogue history
    */
  def history(): IndexedSeq[Utterance]


  /**
    *
    * @return the sequence of locutors by their speaking order
    */
  def locutors(): IndexedSeq[String]

  /**
    * Add an utterance and update dialogue history
    *
    * @param utt utterance to add
    */
  def addUtterance(utt: Utterance): Unit

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

  /**
    * Scores an utterance given the current dialogue history
    *
    * It does not modify the dialogue history
    *
    * @param utt utterance to score
    * @return scoring results of the utterance
    */
  def score(utt: Utterance): UtteranceScoring


}

object DialogueHistory {
  def apply(utterances: IndexedSeq[Utterance] = IndexedSeq.empty[Utterance]): DialogueHistory =
    new DialogueHistory() {
      protected val utterances = scala.collection.mutable.Buffer[Utterance]()

      /**
        *
        * @return the dialogue history
        */
      override def history(): IndexedSeq[Utterance] = utterances.toIndexedSeq

      /**
        * Add an utterance and update dialogue history
        *
        * @param utt utterance to add
        */
      override def addUtterance(utt: Utterance): Unit = {
        utterances.addOne(utt)
      }

      /**
        * Scores an utterance given the current dialogue history
        *
        * It does not modify the dialogue history
        *
        * @param lastUtterance utterance to score
        * @return scoring results of the utterance
        */
      override def score(lastUtterance: Utterance): UtteranceScoring = {
        // Addition to a temporary dialogue history the utterance to score
        val currentUtterances = this.utterances.toIndexedSeq :+ lastUtterance
        // Tokenization of utterances
        val tokenizedUtterances = currentUtterances.map(utt => Tokenizer.tokenizeWithoutMarkers(utt.utterance))

        // Building of the lexicon
        // 1- Building the transcoding from locutor to {A, B}
        val locutors = locutorsHelper(currentUtterances)

        val locutor2speaker =
          (for ((locutor, index) <- locutors.zipWithIndex)
            yield ((locutor, if (index % 2 == 0) Speaker.A else Speaker.B))).toMap

        val speaker2string = (for((speaker, locutor) <- locutor2speaker) yield (locutor, speaker)).toMap

        // 2- Building the link between utterance ID and locutor
        val turn2speaker =
          (for ((Utterance(locutor, _), index) <- currentUtterances.zipWithIndex)
            yield ((index, locutor2speaker(locutor)))).toMap

        // 3- Building the shared expression lexicon
        val lexicon = DialogueLexiconBuilder(tokenizedUtterances, turn2speaker, speaker2string)

        // 4- Building the self-expression lexicon
        val lastSpeaker = locutor2speaker(lastUtterance.locutor)
        // Building a sequence containing only the last locutor's utterances and replacing other utterances by an
        // empty one
        val lastSpeakerUtterances = for {
          (tokenizedUtterance, index) <- tokenizedUtterances.zipWithIndex
          speaker = turn2speaker(index)
        } yield {
          if(speaker == lastSpeaker){
            tokenizedUtterance
          } else{
            TokenizedUtterance.empty
          }
        }

        val selfRepetitionLexicon = DialogueLexiconBuilder(
          lastSpeakerUtterances,
          turn2speaker, speaker2string,
          ExpressionType.OWN_REPETITION_ONLY)

        // 5- Computation of metrics
        val lexiconMetrics = LexiconBasedMeasures(lexicon)
        val selfRepetitionMetrics = SelfRepetitionMeasures(selfRepetitionLexicon)

        UtteranceScoring(lastUtterance,
          lexiconMetrics.expressionRepetition,
          selfRepetitionMetrics.selfExpressionRepetition,
          lexiconMetrics.sharedExpressions,
          lexiconMetrics.establishedSharedExpressions,
          selfRepetitionMetrics.dialogueStructures)
      }


      protected def locutorsHelper(utterances: IndexedSeq[Utterance]): IndexedSeq[String] = {
        val result = scala.collection.mutable.Buffer[String]()
        val alreadySeenSpeakers = scala.collection.mutable.Set[String]()

        for (Utterance(locutor, _) <- utterances) {
          if (!alreadySeenSpeakers.contains(locutor)) {
            result.addOne(locutor)
            alreadySeenSpeakers.addOne(locutor)
          }
        }

        result.toIndexedSeq
      }

      /**
        *
        * @return the sequence of locutors by their speaking order
        */
      override def locutors(): IndexedSeq[String] = locutorsHelper(history())

    }
}