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
package dialign

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

sealed trait CorpusLexicon {
  def expressions(): Set[Expression]

  def corpusFreq(expr: Expression): Int

  def utteranceFreq(expr: Expression): Int

  def mkHierarchicalInventory: String
}

/**
  * Created by gdd on 15/03/17.
  */
object CorpusLexicon extends LazyLogging {
  def aggregate(lexicons: Iterable[DialogueLexicon]): CorpusLexicon = {
    val expr2ID = mutable.Map[Expression, Int]()
    val id2corpusFreq = ArrayBuffer[Int]()
    val id2utteranceFreq = ArrayBuffer[Int]()
    /*
     * Count the number of dialogue where RSTP with id 'index' is used by two speakers
     */
    val id2interUsageFreq = ArrayBuffer[Int]()
    /*
     * Count the number of dialogue where RSTP with id 'index' is used by one speaker
     */
    val id2ownUsageFreq = ArrayBuffer[Int]()

    for {
      lexicon <- lexicons
      expression <- lexicon.expressions
    } {
      val dialogueFreq = lexicon.freq(expression)
      val numberOfDifferentSpeaker = lexicon.numberOfDifferentSpeakers(expression)
      val surfaceForm = expression.toString

      if (!expr2ID.contains(expression)) { // New expression
        // Initialisation of frequency counter
        id2corpusFreq.append(0)
        id2utteranceFreq.append(0)
        id2interUsageFreq.append(0)
        id2ownUsageFreq.append(0)
        // Registering the id
        val id = id2corpusFreq.length - 1
        expr2ID(expression) = id
      }

      // Updating frequencies
      val id = expr2ID(expression)
      id2corpusFreq(id) += 1
      id2utteranceFreq(id) += dialogueFreq
      numberOfDifferentSpeaker match {
        case 1 =>
          id2ownUsageFreq(id) += 1

        case 2 =>
          id2interUsageFreq(id) += 1

        case _ =>
          logger.warn(s"Number of different speakers is invalid for: RSTP($surfaceForm, $dialogueFreq, $numberOfDifferentSpeaker)")
      }
    }

    new CorpusLexicon {
      def corpusFreq(expr: Expression) =
        expr2ID.get(expr) match {
          case None => 0
          case Some(id) => id2corpusFreq(id)
        }

      def utteranceFreq(expr: Expression) =
        expr2ID.get(expr) match {
          case None => 0
          case Some(id) => id2utteranceFreq(id)
        }

      def speakerUsage(expr: Expression): Int = {
        expr2ID.get(expr) match {
          case None => 0
          case Some(id) =>
            if (id2ownUsageFreq(id) > id2interUsageFreq(id)) {
              1
            } else {
              2
            }
        }
      }

      def ownUsageProportion(expr: Expression): Double = {
        expr2ID.get(expr) match {
          case None => 0.0d
          case Some(id) =>
            val total = id2ownUsageFreq(id) + id2interUsageFreq(id)
            id2ownUsageFreq(id) / total.toDouble
        }
      }

      def interUsageProportion(expr: Expression): Double = {
        expr2ID.get(expr) match {
          case None => 0.0d
          case Some(id) =>
            val total = id2ownUsageFreq(id) + id2interUsageFreq(id)
            id2interUsageFreq(id) / total.toDouble
        }
      }

      def expressions(): Set[Expression] =
        expr2ID.keySet.toSet

      def mkHierarchicalInventory: String = {
        val buffer = new StringBuilder()

        // TODO the following code might be subject to optimization in the future
        val sortedRSTP = expressions().toList.sortWith({
          case (rstp1, rstp2) =>
            if (rstp1.content.size > rstp2.content.size) {
              // sorting by size DESC
              true
            } else if (rstp1.content.size < rstp2.content.size) {
              false
            } else {
              // equal size
              if (corpusFreq(rstp1) > corpusFreq(rstp2)) {
                // sorting by freq DESC
                true
              } else if (corpusFreq(rstp1) < corpusFreq(rstp2)) {
                false
              } else {
                // equal freq
                val rstp1seq = rstp1.content.mkString(" ")
                val rstp2seq = rstp2.content.mkString(" ")
                rstp1seq <= rstp2seq // sorting by surface text form ASC
              }
            }
        })

        buffer.append(CSVUtils.mkCSV(List("Corpus Freq.", "Dialogue Freq.", "Size", "Surface Form", "Main Usage", "Own Repetition (%)", "Inter Repetition (%)")))
        buffer.append("\n")
        for (rstp <- sortedRSTP) {
          buffer.append(CSVUtils.mkCSV(List(corpusFreq(rstp), utteranceFreq(rstp),
            rstp.content.size, rstp.content.mkString(" "),
            speakerUsage(rstp), ownUsageProportion(rstp), interUsageProportion(rstp)
          )))
          buffer.append("\n")
        }

        buffer.result()
      }
    }
  }

}
