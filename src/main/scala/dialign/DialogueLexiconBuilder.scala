/*
 * Copyright ISIR, CNRS
 *
 * Contributor(s) :
 *    Guillaume Dubuisson Duplessis <gdubuisson@isir.upmc.fr> (2017)
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
package dialign

import com.typesafe.scalalogging.LazyLogging
import dialign.nlp.Tokenizer
import dialign.nlp.Tokenizer.TokenizedUtterance
import gstlib.GeneralizedSuffixTree

import scala.collection.immutable.SortedSet
import scala.collection.mutable

/**
  * Dialogue lexicon builder from the transcript of a dyadic dialogue
  *
  */
object DialogueLexiconBuilder extends LazyLogging {

  import Speaker._

  def getSpeakerDefault(turnID: Int): Speaker =
    if (turnID % 2 == 0) {
      A
    } else {
      B
    }

  def getSpeakerStrReprDefault(speaker: Speaker): String =
    if(speaker == A) {
      "A"
    } else {
      "B"
    }

  protected def isValidToken(token: String): Boolean =
    token != Tokenizer.BEGIN_MARKER &&
      token != Tokenizer.END_MARKER &&
      token.exists(_.isLetter)

  protected def isValidSequenceDefault(seq: TokenizedUtterance): Boolean =
    seq.size >= 1 && seq.exists(isValidToken(_))

  object ExpressionType extends Enumeration {
    type ExpressionType = Value
    val ALL, OWN_REPETITION_ONLY, INTER_REPETITION_ONLY = Value
  }

  import ExpressionType._

  /**
    * Builds a dialogue lexicon from the tokenized and normalized transcript of a dialogue
    *
    * @param utterances      a sequence of already tokenized and normalized text utterances
    * @param turnID2Speaker  a mapping from a utterance ID in the sequence of utterances and the speaker of this utterance
    * @param mode            type of pattern that are extracted
    * @param isValidSequence utterance validator that tells whether an utterance is eligible to the sequence mining task
    * @return a lexicon of expressions extracted from the dialogue utterances
    */
  def apply(utterances: IndexedSeq[TokenizedUtterance],
            turnID2Speaker: Int => Speaker = getSpeakerDefault,
            speaker2string: Speaker => String = getSpeakerStrReprDefault,
            mode: ExpressionType = INTER_REPETITION_ONLY,
            isValidSequence: TokenizedUtterance => Boolean = isValidSequenceDefault): DialogueLexicon = {
    // Pre-processing: building the generalized suffix tree
    val gstree = GeneralizedSuffixTree(utterances: _*)

    /**
      * Pre-processing: filtering expressions
      */
    def filterExpressions(): (Set[Expression],
      Map[Expression, SortedSet[Int]],
      Map[Int, Map[Expression, SortedSet[Int]]],
      Map[Int, Map[Expression, SortedSet[Int]]],
      Map[Expression, Int],
      Map[Expression, Int]) = {
      /*
       * Reading and storing subsequences ordered by decreasing number of tokens along with their positions in
       * the dialogue
       *
       * Removing patterns used by only one speaker
       */
      val orderedSubsequences = mutable.PriorityQueue[Expression]()(Ordering.by[Expression, Int](r => r.content.size))
      val expr2positions = mutable.Map[Expression, SortedSet[(Int, Int)]]()
      val expr2freq = mutable.Map[Expression, Int]()
      val expr2numSpeaker = mutable.Map[Expression, Int]()

      logger.debug(s"Phase 1: removing patterns following mode $mode or not containing at least one word"

      )
      for ((freq, subseq) <- gstree.bulkMultipleCommonSubsequence()) {
        val subseqStr = subseq.mkString(" ")
        if (isValidSequence(subseq)) {
          val positions = gstree.find(subseq)

          // TODO the following code might be optimized in the future
          val positionsBySpeaker = positions.groupBy({
            case (turnID, _) => turnID2Speaker(turnID)
          })
          val numberOfSpeaker = positionsBySpeaker.size


          mode match {
            case ALL =>
              val expr = Expression(subseq)
              orderedSubsequences += expr
              expr2positions(expr) = (SortedSet[(Int, Int)]() ++ positions)
              expr2freq(expr) = freq
              expr2numSpeaker(expr) = numberOfSpeaker

            case INTER_REPETITION_ONLY =>
              val hasBeenUsedByTwoSpeaker = (numberOfSpeaker > 1)
              if (hasBeenUsedByTwoSpeaker) {
                val expr = Expression(subseq)
                orderedSubsequences += expr
                expr2positions(expr) = (SortedSet[(Int, Int)]() ++ positions)
                expr2freq(expr) = freq
                expr2numSpeaker(expr) = numberOfSpeaker
              } else {
                logger.debug(s"Ignoring pattern ($freq, $subseqStr) because it has only been used by one speaker.")
              }

            case OWN_REPETITION_ONLY =>
              val hasBeenUsedByOneSpeakerOnly = (numberOfSpeaker == 1)
              if (hasBeenUsedByOneSpeakerOnly) {
                val expr = Expression(subseq)
                orderedSubsequences += expr
                expr2positions(expr) = (SortedSet[(Int, Int)]() ++ positions)
                expr2freq(expr) = freq
                expr2numSpeaker(expr) = numberOfSpeaker
              } else {
                logger.debug(s"Ignoring pattern ($freq, $subseqStr) because it has only been used by more than one speaker.")
              }
          }

        } else {
          logger.debug(s"Ignoring pattern ($freq, $subseqStr) because it does not contain any word.")
        }
      }

      val subsequences = mutable.Set[Expression]()

      /*
      * Filtering subsequences to remove useless suffixes of subsequences
       */
      logger.debug(s"Phase 2: removing patterns that are always constrained (i.e., enclosed in another one)")
      val turnID2expr2startingPos = mutable.Map[Int, Map[Expression, SortedSet[Int]]]()
      val turnID2expr2startingPosConstrained = mutable.Map[Int, Map[Expression, SortedSet[Int]]]()

      def existsBiggerPattern(turnID: Int, expr: Expression, startingPos: Int): Boolean = {
        turnID2expr2startingPos.getOrElse(turnID, Map.empty).exists({
          case (otherExpr, pos) =>
            otherExpr.content.size >= expr.content.size && // other expression can include the given expression
              pos.exists(otherStartingPos => otherStartingPos <= startingPos && // there exists an overlapping inclusion
                (otherStartingPos + otherExpr.content.size) >= (startingPos + expr.content.size))
        })
      }

      val subsequencesAll = mutable.Set[Expression]() // TODO the following code might be optimized in the future
      for (expr <- orderedSubsequences.dequeueAll[Expression]) {
        // subsequences are ordered by size
        subsequencesAll += expr

        val positions = expr2positions(expr)
        // For each position of a pattern in a given turn, register the pattern has being either constrained or free
        for ((seqID, startingPos) <- positions) {
          if (existsBiggerPattern(seqID, expr, startingPos)) {
            // pattern is constrained in another one
            if (!turnID2expr2startingPosConstrained.contains(seqID)) {
              turnID2expr2startingPosConstrained(seqID) = Map.empty
            }

            val currentSet = turnID2expr2startingPosConstrained(seqID).getOrElse(expr, SortedSet.empty[Int])
            turnID2expr2startingPosConstrained(seqID) += (expr -> (currentSet + startingPos))

          } else {
            // pattern is free
            if (!subsequences.contains(expr)) {
              logger.debug(s"Adding pattern ($expr)")
            }
            subsequences += expr // saving the pattern as being one to be considered (i.e., appearing as a free one)

            if (!turnID2expr2startingPos.contains(seqID)) {
              turnID2expr2startingPos(seqID) = Map.empty
            }

            val currentSet = turnID2expr2startingPos(seqID).getOrElse(expr, SortedSet.empty[Int])
            turnID2expr2startingPos(seqID) += (expr -> (currentSet + startingPos))
          }
        }
      }

      // Ignoring patterns that are only appearing in a constrained setup
      for {
        ignoredSubsequences <- (subsequencesAll.diff(subsequences))
      } {
        logger.debug(s"Ignoring pattern ($ignoredSubsequences) because it is enclosed in another one")
        expr2positions -= ignoredSubsequences
      }

      def appearsAtLeastOnceFreeExpression(e: Expression): Boolean =
        expr2positions.contains(e)


      (subsequences.filter(appearsAtLeastOnceFreeExpression(_)).toSet,
        expr2positions.view.mapValues(seqIDandPos => seqIDandPos.map(_._1)).toMap,
        turnID2expr2startingPos.view.mapValues(_.view.filterKeys(appearsAtLeastOnceFreeExpression(_)).toMap).toMap,
        turnID2expr2startingPosConstrained.view.mapValues(_.view.filterKeys(appearsAtLeastOnceFreeExpression(_)).toMap).toMap,
        expr2freq.view.filterKeys(appearsAtLeastOnceFreeExpression(_)).toMap,
        expr2numSpeaker.view.filterKeys(appearsAtLeastOnceFreeExpression(_)).toMap
      )
    }

    // Extraction of expressions
    val (expressions,
    expr2turnID, turnID2expr2startingPos, turnID2expr2startingPosConstrained,
    expr2freq, expr2numSpeakers) = filterExpressions()

    new DialogueLexicon(expressions, expr2turnID, expr2freq, expr2numSpeakers,
      turnID2expr2startingPos, turnID2expr2startingPosConstrained,
      utterances, turnID2Speaker, speaker2string)
  }
}
