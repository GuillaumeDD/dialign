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
import dialign.Speaker.Speaker
import dialign.nlp.Tokenizer.TokenizedUtterance

import scala.collection.immutable.SortedSet
import scala.collection.immutable

/**
  * Represents a dialogue lexicon of expressions
  *
  * @param expressions                        the set of expressions appearing in the dialogue
  * @param expr2turnID                        a mapping from expression to the turns in which they appear (either free or constrained)
  * @param expr2freq                          a mapping from expression to its frequency
  * @param expr2numSpeakers                   a mapping from expression to its number of speaker
  * @param turnID2expr2startingPos            a mapping from turn ID to *free* expressions to the starting position (of a given
  *                                           expression in a given turn)
  * @param turnID2expr2startingPosConstrained a mapping from turn ID to *constrained* expressions to the starting
  *                                           position (of a given expression in a given turn)
  * @param utterances                         the indexed sequence of turns constituting this dialogue
  * @param turnID2Speaker                     a mapping from a turn index to its speaker
  */
class DialogueLexicon(
                       val expressions: Set[Expression],
                       private val expr2turnID: Map[Expression, SortedSet[Int]],
                       private val expr2freq: Map[Expression, Int],
                       private val expr2numSpeakers: Map[Expression, Int],
                       private val turnID2expr2startingPos: Map[Int, Map[Expression, SortedSet[Int]]],
                       private val turnID2expr2startingPosConstrained: Map[Int, Map[Expression, SortedSet[Int]]],
                       utterances: IndexedSeq[TokenizedUtterance],
                       turnID2Speaker: Int => Speaker,
                       speaker2string: Speaker => String
                     ) extends LazyLogging {
  lexicon =>

  import Speaker.{A, B}

  def exprsInitialisedBy(speaker: Speaker): Set[Expression] =
    for {
      expr <- expressions
      if expr.firstSpeaker() == speaker
    } yield (expr)

  /**
    * Computes the speaker of a turn given its index
    *
    * @param turnID index of the turn
    * @return speaker of the turn
    */
  def getSpeaker(turnID: Int): Speaker = turnID2Speaker(turnID)

  /**
    * Computes the speaker string representation of a turn given its index
    *
    * @param turnID index of the turn
    * @return string representation of the speaker of the turn
    */
  def getSpeakerStrRepr(turnID: Int) : String = speaker2string(getSpeaker(turnID))

  /**
    *
    * @return a pair (string representation for speaker A, string representation for speaker B)
    */
  def rawSpeakers(): (String, String) =
      (speaker2string(A), speaker2string(B))

  /**
    * Computes the set of free expressions included in a given turn
    *
    */
  private def turnID2expr(turnID: Int): Set[Expression] =
    turnID2expr2startingPos.getOrElse(turnID, Map.empty).keySet

  /**
    * Computes the set of starting positions of a given free expressions included in a given turn
    *
    */
  private def turnIDAndExpr2startingPos(turnID: Int, expr: Expression): SortedSet[Int] =
    turnID2expr2startingPos.getOrElse(turnID, Map.empty).getOrElse(expr, SortedSet.empty)

  /**
    * Computes the set of constrained expressions included in a given turn
    *
    */
  private def turnID2exprConstrained(turnID: Int): Set[Expression] =
    turnID2expr2startingPosConstrained.getOrElse(turnID, Map.empty).keySet

  /**
    * Computes the set of starting positions of a constrained expressions included in a given turn
    *
    */
  private def turnIDAndExpr2startingPosConstrained(turnID: Int, expr: Expression): SortedSet[Int] =
    turnID2expr2startingPosConstrained.getOrElse(turnID, Map.empty).getOrElse(expr, SortedSet.empty)

  /*
   * CONTEXTUALISED EXPRESSION
   */
  /**
    * Computes the number of utterances in which the given expression appears (in a free or constrained form)
    *
    */
  def freq(expr: Expression): Int = expr.freq


  def numberOfDifferentSpeakers(expr: Expression): Int = expr.numberOfDifferentSpeakers

  /**
    * Extension methods for expression in the context of this dialogue
    *
    * @param expr
    */
  implicit class ContextualisedExpression(expr: Expression) {
    /**
      * Computes the number of utterances in which the given expression appears (in a free or constrained form)
      *
      */
    def freq: Int = lexicon.expr2freq(expr)

    /**
      * Computes the number of utterances in which the given expression appears in a *free* form
      *
      * @note This operation is not efficient.
      */
    def freeFreq(): Int = {
      val numberOfAppearances =
        for {
          turnID <- expr2turnID(expr).toList // Set to List to avoid surprise when mapping to the number of starting positions
          startingPos = turnID2expr2startingPos(turnID).getOrElse(expr, SortedSet.empty[Int])
        } yield {
          if (startingPos.size > 0) {
            1
          } else {
            0
          }
        }

      numberOfAppearances.sum
    }

    /**
      * Computes the number of occurrences in which the given expression appears in a *free* form
      *
      * It may appear several times in the same utterance.
      *
      * @note This operation is not efficient.
      */
    def freeFreqFromPos(): Int = {
      val numberOfAppearances =
        for {
          turnID <- expr2turnID(expr).toList // Set to List to avoid surprise when mapping to the number of starting positions
          startingPos = turnID2expr2startingPos(turnID).getOrElse(expr, SortedSet.empty[Int])
        } yield {
          startingPos.size
        }

      numberOfAppearances.sum
    }

    def numberOfDifferentSpeakers: Int = lexicon.expr2numSpeakers(expr)

    /**
      * The set of turns in which the expression appears (either free or constrained)
      *
      */
    def turns(): SortedSet[Int] = lexicon.expr2turnID(expr)

    /**
      * Size (in turns) between the first and the last appearance of this expression
      * (including the first and last turns)
      *
      */
    def span(): Int = {
      val turnIndexes = turns()
      turnIndexes.last - turnIndexes.head + 1
    }

    /**
      * Computes the establishement turn index of this expression
      *
      */
    def establishementTurnID(): Int = {
      // TODO the following code might be subject to optimization in the future (memoization?)
      var established = false
      var speaker1 = false
      var speaker2 = false
      var freeFormUsage = false

      val it = turns().iterator
      var turnID = -1
      while (it.hasNext && !established) {
        turnID = it.next()
        getSpeaker(turnID) match {
          case A => speaker1 = true
          case B => speaker2 = true
        }

        val turn = lexicon.turns(turnID)
        if (turn.freeExpressions.contains(expr)) {
          freeFormUsage = true
        }

        established = speaker1 && speaker2 && freeFormUsage
      }
      if (!established) {
        logger.debug(s"Unable to compute establishment turn ID for: $expr")
        lexicon.turns.size
      } else {
        turnID
      }
    }

    def priming(): Int = {
      val establishementTurn = establishementTurnID()
      val firstProducer = firstSpeaker()
      turns()
        .filter(turn => turn < establishementTurn)
        .takeWhile(getSpeaker(_) == firstProducer)
        .size
    }

    def firstSpeaker(): Speaker = {
      getSpeaker(turns().head)
    }

    def firstSpeakerRepr(): String = speaker2string(firstSpeaker())

    /**
      * Computes the ranges of token IDs involved in this expression for a given turn
      *
      */
    def ranges(turnID: Int): Seq[Range] =
      for (startingPos <- lexicon.turnIDAndExpr2startingPos(turnID, expr).toSeq)
        yield (startingPos to (startingPos + expr.content.size - 1)) // beware of the '-1'

    /**
      * Number of instances of this expression that are free
      *
      */
    def freeRatio(): Double = {
      // TODO the following code might be subject to optimization in the future
      var nbFree = 0
      var nbConstrained = 0

      for (turn <- lexicon.turns) {
        nbFree += lexicon.turnIDAndExpr2startingPos(turn.id, expr).size
        nbConstrained += lexicon.turnIDAndExpr2startingPosConstrained(turn.id, expr).size
      }

      nbFree.toDouble / (nbFree + nbConstrained)
    }

    def completeString(): String = {
      val turns2pos = {
        for (turnID <- lexicon.expr2turnID(expr))
          yield (s"""\n\tturn=$turnID: free pos=${lexicon.turnIDAndExpr2startingPos(turnID, expr).mkString("(", ", ", ")")} ; constr. pos=${lexicon.turnIDAndExpr2startingPosConstrained(turnID, expr).mkString("(", ", ", ")")}""")
      }.mkString
      s"Pattern=${expr.content.mkString(" ")} (freq=${expr.freq}, spanning=${expr.span()}, first speaker=${expr.firstSpeaker()}, establishment turn=${expr.establishementTurnID()}) ; $turns2pos"
    }
  }

  /*
   * DIALOGUE TURNS
   *
   */
  /**
    * Represent a dialogue turn
    *
    */
  case class Turn(id: Int) {
    def speaker: Speaker = getSpeaker(id)
    def rawSpeaker: String = getSpeakerStrRepr(id)

    def content: TokenizedUtterance = utterances(id)

    /**
      * Computes the set of free expressions appearing in this turn
      *
      */
    def freeExpressions: Set[Expression] = turnID2expr(this.id)

    /**
      * Computes the set of constrained expressions appearing in this turn
      *
      */
    def constrainedExpressions: Set[Expression] = turnID2exprConstrained(this.id)

    /**
      * Computes the set of expressions appearing in this turn (either in a free
      * or constrained form)
      *
      */
    def allExpressions: Set[Expression] =
      freeExpressions ++ constrainedExpressions

    def tokenSize: Int = content.size

    /**
      * Computes the ratio of tokens belonging to an *established* expression in this turn
      *
      * @see rawExprsTokenSize method is a similar method that does not take into account expression establishment
      *
      */
    lazy val exprsTokenSize: Int = {
      // Recovering the ranges of token positions involved in an expression
      val flatRanges =
        for {
          expr <- this.freeExpressions
          if this.id >= expr.establishementTurnID() // only count expressions that are established
          range <- expr.ranges(this.id)
        } yield (range)

      // Recovering and counting token involved in an expression
      (for {
        i <- 0 until tokenSize
        if flatRanges.exists(range => range.contains(i))
      } yield (i)).size
    }

    /**
      * Computes the ratio of tokens belonging to an expression (without taking into account establishment) in this turn
      *
      * @see exprsTokenSize method is a similar method that takes into account expression establishment
      */
    lazy val rawExprsTokenSize: Int = {
      // Recovering the ranges of token positions involved in an expression
      val flatRanges =
        for {
          expr <- this.freeExpressions
          range <- expr.ranges(this.id)
        } yield (range)

      // Recovering and counting token involved in an expression
      (for {
        i <- 0 until tokenSize
        if flatRanges.exists(range => range.contains(i))
      } yield (i)).size
    }

    override def toString: String =
      s"""Turn ID=$id| $speaker | $rawSpeaker : ${content.mkString(" ")}
                     |\tFree: ${freeExpressions.map(_.mkString).mkString(", ")}
                     |\tConstrained: ${constrainedExpressions.map(_.mkString).mkString(", ")}""".stripMargin
  }

  /**
    * Computes the range of turn indexes in the dialogue
    *
    */
  def turnIDs(): Range = utterances.indices

  /**
    * Sequence of turns in this dialogue
    *
    */
  val turns: immutable.IndexedSeq[Turn] = turnIDs().map(Turn(_))

  /*
   * STATS
   */
  /**
    * Total number of tokens in this dialogue
    */
  lazy val nbTokens: Int =
    turns.map(_.tokenSize).sum

  /**
    * Number of tokens produced by a given speaker
    *
    */
  def nbTokensBy(speaker: Speaker): Int =
    turns.filter(_.speaker == speaker).map(_.tokenSize).sum
}

object DialogueLexicon {
  /**
    * Builds a hierarchical inventory string representation for other-repetitions
    *
    */
  def mkHierarchicalInventory(lexicon: DialogueLexicon): String = {
    import lexicon._

    val buffer = new StringBuilder()

    // TODO the following code might be subject to optimization in the future
    val sortedExpressions = lexicon.expressions.toList.sortWith({
      case (expr1, expr2) =>
        if (expr1.content.size > expr2.content.size) {
          // sorting by size DESC
          true
        } else if (expr1.content.size < expr2.content.size) {
          false
        } else {
          // equal size
          if (expr1.freq > expr2.freq) {
            // sorting by freq DESC
            true
          } else if (expr1.freq < expr2.freq) {
            false
          } else {
            // equal freq
            val expr1seq = expr1.content.mkString(" ")
            val expr2seq = expr2.content.mkString(" ")
            expr1seq <= expr2seq // sorting by surface text form ASC
          }
        }
    })

    buffer.append(CSVUtils.mkCSV(List("Freq.", "Free Freq.", "Size", "Surface Form",
      "Establishment turn", "Spanning", "Priming", "First Speaker",
      "Turns")))
    buffer.append("\n")
    for (expr <- sortedExpressions) {
      val establishementTurn = expr.establishementTurnID()

      buffer.append(CSVUtils.mkCSV(List(expr.freq, expr.freeFreq(), expr.content.size, expr.content.mkString(" "),
        establishementTurn, expr.span(), expr.priming(), expr.firstSpeakerRepr(),
        expr.turns().mkString(", "))))
      buffer.append("\n")
    }

    buffer.result()
  }

  /**
    * Builds a hierarchical inventory string representation for self-repetitions
    *
    */
  def mkSelfRepetitionHierarchicalInventory(lexicon: DialogueLexicon): String = {
    import lexicon._

    val buffer = new StringBuilder()

    // TODO the following code might be subject to optimization in the future
    val sortedExpressions = lexicon.expressions.toList.sortWith({
      case (expr1, expr2) =>
        if (expr1.content.size > expr2.content.size) {
          // sorting by size DESC
          true
        } else if (expr1.content.size < expr2.content.size) {
          false
        } else {
          // equal size
          if (expr1.freq > expr2.freq) {
            // sorting by freq DESC
            true
          } else if (expr1.freq < expr2.freq) {
            false
          } else {
            // equal freq
            val expr1seq = expr1.content.mkString(" ")
            val expr2seq = expr2.content.mkString(" ")
            expr1seq <= expr2seq // sorting by surface text form ASC
          }
        }
    })

    buffer.append(CSVUtils.mkCSV(List("Freq.", "Size", "Surface Form",
      "Spanning", "First Speaker", "Turns")))
    buffer.append("\n")
    for (expr <- sortedExpressions) {
      buffer.append(CSVUtils.mkCSV(List(expr.freq, expr.content.size, expr.content.mkString(" "),
        expr.span(), expr.firstSpeakerRepr(), expr.turns().mkString(", "))))
      buffer.append("\n")
    }

    buffer.result()
  }

  def mkStringTurns(lexicon: DialogueLexicon): String = {
    import lexicon._

    val buffer = new StringBuilder
    for ((turn, index) <- turns.zipWithIndex) {
      buffer.append(s"${turn.speaker}|$index| ${turn.rawSpeaker}: ")

      // Recovering the ranges of token positions involved in an expression
      val flatRanges =
        for {
          expr <- turn.freeExpressions
          if turn.id >= expr.establishementTurnID() // only count expressions that are established
          range <- expr.ranges(turn.id)
        } yield (range)

      val flatRangesUnestablished =
        for {
          expr <- turn.freeExpressions
          if turn.id < expr.establishementTurnID() // only count expressions that are not established
          range <- expr.ranges(turn.id)
        } yield (range)

      for ((token, i) <- turn.content.zipWithIndex) {
        val startCount = flatRanges.count(range => range.start == i)
        val start = "[" * startCount

        val endCount = flatRanges.count(range => range.end == i)
        val end = "]" * endCount

        val startCountUnestablished = flatRangesUnestablished.count(range => range.start == i)
        val endCountUnestablished = flatRangesUnestablished.count(range => range.end == i)
        val startUnestablished = "_" * startCountUnestablished
        val endUnestablished = "_" * endCountUnestablished

        buffer.append(s"$start$startUnestablished$token$endUnestablished$end ")
      }
      buffer.append("\n")
    }

    buffer.result()
  }
}
