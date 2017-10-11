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
package dialign.metrics.offline

import dialign.Speaker.Speaker
import dialign._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * Computes verbal alignment measures and vocabulary overlap from a dialogue lexicon
  *
  */
case class DialogueLexiconMeasures(lexicon: DialogueLexicon) {

  /*
   * Helpers
   */
  private val turnID2expressionRepetition: Array[Double] = {
    val result = Array.ofDim[Double](lexicon.turns.size)
    var nbTokens = 0
    var nbTokensInRSTP = 0
    var i = 0
    for (turn <- lexicon.turns) {
      nbTokens += turn.tokenSize
      nbTokensInRSTP += turn.exprsTokenSize
      result(i) = (nbTokensInRSTP.toDouble / nbTokens.toDouble)
      i += 1
    }

    result
  }

  protected def reduceByKey[K, V](pairSeq: Iterable[(K, V)], f: (V, V) => V): Map[K, V] = {
    val m = mutable.Map[K, V]()

    @tailrec
    def reduce(seq: Iterable[(K, V)]): Unit = {
      if (seq.nonEmpty) {
        val (key, value) = seq.head

        if (m.contains(key)) {
          m(key) = f(m(key), value)
        } else {
          m(key) = value
        }

        reduce(seq.tail)
      }
    }

    reduce(pairSeq)

    m.toMap
  }

  protected def log2(x: Double): Double =
    Math.log10(x) / Math.log10(2.0d)

  private case class SharedVocabulary(vocabularies: Map[Speaker, Set[String]]) {
    lazy val union: Set[String] = vocabularies.values.reduceLeft(_ union _)
    lazy val intersect: Set[String] = vocabularies.values.reduceLeft(_ intersect _)

    /**
      * Shared vocabulary between speaker
      */
    lazy val sv: Double = intersect.size.toDouble / union.size.toDouble

    def sv(speaker: Speaker): Double = intersect.size.toDouble / vocabularies(speaker).size.toDouble
  }

  private val sharedVocabularyHelper = {
    // Initialisation of vocabulary
    val speaker2vocabulary: Map[Speaker, mutable.Set[String]] =
      (for (speaker <- Speaker.values.toList)
        yield (speaker, mutable.Set[String]())).toMap

    // Building vocabularies
    for (turn <- lexicon.turns) {
      speaker2vocabulary(turn.speaker) ++= turn.content
    }
    SharedVocabulary(speaker2vocabulary.mapValues(_.toSet))
  }

  private case class SpeakerSummary(speaker: Speaker,
                                    lexiconNbToken: Int,
                                    lexiconNbRSTP: Int,
                                    nbRSTPUsed: Int,
                                    rstpUsageVariety: Double,
                                    nbRSTPInitialised: Int,
                                    nbTokens: Int,
                                    nbTokensInRSTP: Int) {
    val expressionRepetition: Double = nbTokensInRSTP.toDouble / nbTokens.toDouble

    override def toString: String =
      s"""Speaker summary for $speaker:
         |\tnb instances of expressions used: $nbRSTPUsed
         |\tproportion of expression lexicon used in a free form (%): $rstpUsageVariety
         |\tnb expression initialised: $nbRSTPInitialised (ratio=${nbRSTPInitialised.toDouble / lexiconNbRSTP})
         |\tnb tokens: $nbTokens (ratio=${nbTokens.toDouble / lexiconNbToken})
         |\tnb tokens in established expression: $nbTokensInRSTP
         |\tcoverage (%): $expressionRepetition (nb tokens in an established expression)
       """.stripMargin
  }

  private val speaker2stats: Map[dialign.Speaker.Value, SpeakerSummary] =
    (for (speaker <- Speaker.values)
      yield {
        var nbRSTPUsed = 0
        var nbTokens = 0
        var nbTokensInRSTP = 0
        val rstps = mutable.Set[Expression]()
        for {
          turn <- lexicon.turns
          if turn.speaker == speaker
        } {
          nbRSTPUsed += turn.freeExpressions.size
          nbTokens += turn.tokenSize
          nbTokensInRSTP += turn.exprsTokenSize
          rstps ++= turn.freeExpressions
        }

        val nbRSTPInitialised = lexicon.exprsInitialisedBy(speaker).size
        val rstpUsageVariety = rstps.size.toDouble / lexicon.expressions.size

        (speaker, SpeakerSummary(speaker, lexicon.nbTokens, lexicon.expressions.size,
          nbRSTPUsed, rstpUsageVariety, nbRSTPInitialised, nbTokens, nbTokensInRSTP))
      }).toMap

  /*
   * Measures and some stats
   *
   */
  lazy val numUtterances: Int = lexicon.turns.size

  lazy val numTokens: Int = lexicon.nbTokens

  lazy val numExpressions: Int = lexicon.expressions.size

  /*
    * Mapping: size (in tokens) -> number of instances of *free* expr. with that pattern
    */
  protected lazy val size2freq: Map[Int, Int] = {
    import lexicon._

    /*
     * Paired iterable of (expr. length in tokens, freq. of expr. of the given lenght)
     *
     */
    val pairedIterable = lexicon.expressions
      .toList // from set to list to avoid disappearing pairs
      .map(e => (e.content.length, e.freeFreq)) // here: selection of free expression


    reduceByKey[Int, Int](pairedIterable, _ + _)
  }

  /**
    * Average length of the instances of expression
    *
    */
  lazy val L: Double = {
    var m = 0.0d
    var total = 0.0d

    for ((size, freq) <- size2freq) {
      m += size * freq
      total += freq
    }

    m / total
  }

  /**
    * Size (in number of tokens) of the biggest expression
    */
  lazy val LMAX: Int = {
    val exprSizes = lexicon.expressions.map(_.content.size)
    if(exprSizes.nonEmpty){
      exprSizes.max
    } else {
      0
    }
  }

  /**
    * The Shannon entropy of the probability distribution of the expression sizes (in tokens)
    */
  lazy val ENTR: Double = {
    // The Shannon entropy of the probability distribution of the diagonal line lengths

    // Computation of the normalisation ratio (to estimate probabilities from freq.)
    val total = size2freq.values.sum

    val entropyComponents =
      for ((_, freq) <- size2freq)
        yield {
          // Computation of probability p of the given expression size
          val p = freq.toDouble / total.toDouble
          // Component of the entropy
          p * log2(p)
        }

    val entropy = -entropyComponents.sum
    entropy
  }

  lazy val expressionVariety: Double = numExpressions.toDouble / numTokens.toDouble

  /**
    * Expression Repetition (ER) measure for other-repetition
    */
  lazy val expressionRepetition: Double = {
    val lastIndex = numUtterances - 1
    turnID2expressionRepetition(lastIndex)
  }

  /**
    * Expression Repetition (ER) measure for self-repetition
    */
  lazy val expressionSelfRepetition: Double = {
    var nbTokens = 0
    var nbTokensInRSTP = 0

    for (turn <- lexicon.turns) {
      nbTokens += turn.tokenSize
      nbTokensInRSTP += turn.rawExprsTokenSize // beware: rawExprsTokenSize is called and not exprsTokenSize
    }

    (nbTokensInRSTP.toDouble / nbTokens.toDouble)
  }

  def initiatedExpressionsBy(s: Speaker): Double = speaker2stats(s).nbRSTPInitialised.toDouble / numExpressions.toDouble

  def producedTokensBy(s: Speaker): Double = speaker2stats(s).nbTokens / numTokens.toDouble

  def expressionRepetitionBy(s: Speaker): Double = speaker2stats(s).expressionRepetition

  def numUsedExprBy(s: Speaker): Double = speaker2stats(s).nbRSTPUsed

  def exprUsageVarietyBy(s: Speaker): Double = speaker2stats(s).rstpUsageVariety

  /*
   * Shared vocabulary measures
   */
  lazy val sharedVocabulary: Double = sharedVocabularyHelper.sv

  def sharedVocabularyBy(s: Speaker): Double = sharedVocabularyHelper.sv(s)

}

object DialogueLexiconMeasures {

  val headingToCSV: String = {
    val heading = List("ID", "Num. utterances", "Num. tokens",
      "Expression Lexicon Size (ELS)", "Expression Variety (EV)", "Expression Repetition (ER)",
      "S1/Initiated Expression (IE_S1)", "S1/Expression Repetition (ER_S1)", "S1/tokens (%)",
      "S2/Initiated Expression (IE_S2)", "S2/Expression Repetition (ER_S2)", "S2/tokens (%)",
      "Voc. Overlap", "Voc. Overlap S1", "Voc. Overlap S2",
      // Other measures
      "ENTR", "L", "LMAX",
      // Self-repetition measures
      "SR/S1/ELS", "SR/S1/EV", "SR/S1/ER", "SR/S1/ENTR", "SR/S1/L", "SR/S1/LMAX",
      "SR/S2/ELS", "SR/S2/EV", "SR/S2/ER", "SR/S2/ENTR", "SR/S2/L", "SR/S2/LMAX"
    )
    CSVUtils.mkCSV(heading)
  }

  def toCSV(measures: DialogueLexiconMeasures): String = {
    import Speaker.{A, B}
    import measures._

    val data = List(
      // General stats
      numUtterances, numTokens,
      // Expression model measures
      numExpressions, expressionVariety, expressionRepetition,
      // Speaker-specific measures
      initiatedExpressionsBy(A), expressionRepetitionBy(A), producedTokensBy(A),
      initiatedExpressionsBy(B), expressionRepetitionBy(B), producedTokensBy(B),
      // Shared vocabulary measures
      sharedVocabulary, sharedVocabularyBy(A), sharedVocabularyBy(B),
      ENTR, L, LMAX
    )

    CSVUtils.mkCSV(data)
  }

  /**
    * Builds a CSV string representation for ELS, EV and ER measures only
    *
    */
  def toCSVSelfRepetition(measures: DialogueLexiconMeasures): String = {
    import measures._

    val data = List(
      // Expression model measures
      numExpressions, expressionVariety, expressionSelfRepetition, ENTR, L, LMAX
    )

    CSVUtils.mkCSV(data)
  }
}
