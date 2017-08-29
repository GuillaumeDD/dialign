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
package dialign.nlp

object Tokenizer {
  type TokenizedUtterance = Array[String]

  object TokenizedUtterance {
    val empty: TokenizedUtterance = Array("")
  }
  val BEGIN_MARKER = "#B"
  val END_MARKER = "#E"

  private def split(s: String): TokenizedUtterance =
    s.trim.replaceAll("([\\\\.\\\\?!,]+)", " $1 ") // adding space around punctuation: . ? ! ,
      .split(" +")

  /**
    * Tokenized a given utterance by splitting white spaces and by adding
    * begin and end markers
    *
    */
  def tokenize(s: String): TokenizedUtterance =
    s.tokenize

  /**
    * Tokenized a given utterance by splitting white spaces but DO NOT
    * add begin and end markers
    *
    */
  def tokenizeWithoutMarkers(s: String): TokenizedUtterance =
    split(s)

  implicit class Tokenizable(s: String) {
    def tokenize: TokenizedUtterance =
      BEGIN_MARKER +: split(s) :+ END_MARKER
  }

}
