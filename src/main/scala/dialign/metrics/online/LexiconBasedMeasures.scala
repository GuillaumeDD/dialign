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
package dialign.metrics.online

import dialign.DialogueLexicon

case class LexiconBasedMeasures(lexicon: DialogueLexicon) {

  import lexicon._

  private val lastTurnID = turns.size - 1
  private val lastTurn = turns(lastTurnID)
  val sharedExpressions =  lastTurn.allExpressions.filter(_.turns().size > 0)
  val establishedSharedExpressions = expressions.filter(_.establishementTurnID() == lastTurnID)

  /**
    * The expression expression measure of the last turn of the dialogue, i.e.
    * a measure of the lexicon usage
    *
    */
  val expressionRepetition =
    if (lexicon.turns(lastTurnID).tokenSize > 0) {
      lexicon.turns(lastTurnID).exprsTokenSize.toDouble / lexicon.turns(lastTurnID).tokenSize.toDouble
    } else {
      0.0d
    }

  /**
    * A measure of the contribution to the lexicon of the last turn of the dialogue, i.e.
    * the number of established expression in the last turn
    *
    */
  val lexiconContribution = expressions.count(_.establishementTurnID() == lastTurnID)
}
