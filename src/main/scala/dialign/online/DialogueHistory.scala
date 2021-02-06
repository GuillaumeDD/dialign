/*
 * Copyright ISIR, CNRS
 *
 * Contributor(s) :
 *    Guillaume Dubuisson Duplessis <guillaume@dubuissonduplessis.fr> (2017, 2020, 2021)
 *
 * This software is a computer program whose purpose is to implement
 * automatic and generic measures of verbal alignment in dyadic dialogue
 * based on sequential pattern mining at the level of surface of text
 * utterances.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 *
 */
package dialign.online

import dialign.DialogueLexiconBuilder.ExpressionType
import dialign.metrics.online.{LexiconBasedMeasures, SelfRepetitionMeasures}
import dialign.nlp.Tokenizer
import dialign.nlp.Tokenizer.TokenizedUtterance
import dialign.{DialogueLexiconBuilder, Speaker}

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
  def apply(utterances: IndexedSeq[Utterance] = IndexedSeq.empty[Utterance]): DialogueHistory = {
    // Creation and implemenation of the dialogue history
    val result = new DialogueHistory() {
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
        val tokenizedUtterances = currentUtterances.map(utt => Tokenizer.tokenizeWithoutMarkers(utt.text))

        // Building of the lexicon
        // 1- Building the transcoding from locutor to {A, B}
        val locutors = locutorsHelper(currentUtterances)

        val locutor2speaker =
          (for ((locutor, index) <- locutors.zipWithIndex)
            yield ((locutor, if (index % 2 == 0) Speaker.A else Speaker.B))).toMap

        val speaker2string = (for ((speaker, locutor) <- locutor2speaker) yield (locutor, speaker)).toMap

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
          if (speaker == lastSpeaker) {
            tokenizedUtterance
          } else {
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

    // Addition of the provided utterances
    for(utterance <- utterances) {
      result.addUtterance(utterance)
    }

    result
  }
}
