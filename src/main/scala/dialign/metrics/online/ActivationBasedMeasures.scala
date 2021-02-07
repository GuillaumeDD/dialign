/*
 * Copyright ISIR, CNRS
 *
 * Contributor(s) :
 *    Guillaume Dubuisson Duplessis <guillaume@dubuissonduplessis.fr> (2017)
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
package dialign.metrics.online

import com.typesafe.scalalogging.LazyLogging
import dialign.{DialogueLexicon, Expression}

case class ActivationBasedMeasures(lexicon: DialogueLexicon,
                                   alpha: Double = 4.0d,
                                   beta: Double = 1.0d,
                                   lambda: Double = 0.5d)
  extends LazyLogging {

  import lexicon._

  private val lastTurnID = turns.size - 1
  private val lastTurn = turns(lastTurnID)

  /**
    * Dialogue structures considered in the computation of the activation
    */
  val dialogueStructures = lastTurn.allExpressions.filter(_.turns().size > 0)
  /**
    * Number of considered dialogue structures
    */
  val N = dialogueStructures.size


  /*
   * Helpers to compute activation
   */
  def temporaryActivation(deltaTime: Int): Double = {
    val exponent = -(deltaTime.toDouble - 1.0d) / alpha

    Math.exp(exponent)
  }

  def permanentActivation(freq: Int): Double = {
    val exponent = -(freq.toDouble - 1.0d) / beta

    1.0d - Math.exp(exponent)
  }

  /**
    * Computes the number of turns in a dialogue of pairs human utt./system utt.
    *
    */
  private def nbTurns(firstTurn: Int, lastTurn: Int): Int = {
    lastTurn - firstTurn + 1
  }

  private def dialogueStructureWeight(e: Expression): Double =
    e.content.size.toDouble

  private def dialogueStructureDeltaTime(e: Expression): Int = {
    nbTurns(e.turns().head, lastTurnID)
  }

  private def dialogueStructureStrength(e: Expression): Int =
    e.turns().size

  def activation(): Double = {
    logger.debug(s"Computing activation for turn: ${lastTurn.content.mkString(" ")}")

    val consideredExpressions = dialogueStructures

    if (N == 0) {
      0.0d
    } else {
      // Computation of individual activation of each RSTP
      var act = 0.0d
      var normalisation = 0.0d
      for (expression <- consideredExpressions) {
        // Computation of the delta time taking into account this turn ID
        val deltaTime = dialogueStructureDeltaTime(expression)
        assert(deltaTime > 0, s"deltaTime is inferior to 0 (=$deltaTime)")

        // Computation of the frequency of the RSTP until this turn ID
        val freq = dialogueStructureStrength(expression)

        val expressionAct = lambda * temporaryActivation(deltaTime) + (1.0d - lambda) * permanentActivation(freq)
        val weight = dialogueStructureWeight(expression)

        logger.debug(s"\trstp=$expression\n" +
          s"\t\t(dT=$deltaTime|ta=${temporaryActivation(deltaTime)};freq=$freq|pa=${permanentActivation(freq)}; act=$expressionAct)" +
          s"\n\t\tweight=$weight")

        act += weight * expressionAct
        normalisation += weight
      }

      // Computation of final activation
      val expressionRepetition = lastTurn.exprsTokenSize.toDouble / lastTurn.tokenSize.toDouble
      logger.debug(s"\texpression repetition: ${lastTurn.exprsTokenSize} / ${lastTurn.tokenSize} = $expressionRepetition")
      val finalActivation = expressionRepetition * act / normalisation
      logger.debug(s"\t$expressionRepetition * $act / $normalisation = $finalActivation")

      finalActivation
    }
  }
}
