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
package dialign.IO

import java.io.File

import dialign.Speaker
import dialign.Speaker._
import dialign.nlp.Normalizer
import dialign.nlp.Tokenizer.TokenizedUtterance

import scala.collection.mutable

/**
  * Provides helper to read a dialogue from a file
  *
  */
object DialogueReader {

  private val SEPARATOR_FIELD = "\t"

  /**
    * A delexicaliser that does nothing
    */
  protected def defaultDelexicalize(utt: String): String = utt

  /**
    * Represents a pre-processed textual dyadic dialogue
    *
    * @param name            unique name of the dialogue
    * @param utterances      a sequence of already tokenized and normalized text utterances
    * @param turn2rawSpeaker a mapping from a utterance ID in the sequence of utterances and the speaker of this utterance
    */
  case class Dialogue(name: String,
                      utterances: IndexedSeq[TokenizedUtterance],
                      turn2rawSpeaker: Int => String
                     ) {
    val rawSpeakers =
      (for (i <- 0 until utterances.length)
        yield (turn2rawSpeaker(i)))
        .toList
        .distinct
        .sorted
    assert(rawSpeakers.size <= 2, s"Dialogue $name with more than 2 speakers: ${rawSpeakers.mkString(", ")}")

    /**
      * First speaker in this dialogue (aka 'A')
      */
    val firstSpeaker = rawSpeakers.head
    /**
      * Second speaker in this dialogue (aka 'B'
      */
    val secondSpeaker = if (rawSpeakers.size == 2) {
      rawSpeakers.tail.head
    } else {
      "NS"
    }

    protected val rawSpeakers2Speaker = {
      if (rawSpeakers.isEmpty) {
        Map.empty[String, Speaker]
      } else if (rawSpeakers.size == 1) {
        Map(firstSpeaker -> Speaker.A)
      } else {
        Map(
          firstSpeaker -> Speaker.A,
          secondSpeaker -> Speaker.B
        )
      }
    }

    /**
      * A mapping from a utterance ID in the sequence of utterances and the standardized speaker of this utterance
      *
      * @param index index of the utterance in the sequence
      * @return the standardized speaker of this utterance
      */
    def getSpeaker(index: Int): Speaker =
      rawSpeakers2Speaker(turn2rawSpeaker(index))
  }

  /**
    * Loads a text file containing a dialogue
    *
    * @param file         input file containing the dialogue transcript
    * @param tokenize     a straightforward tokenizer
    * @param delexicalize a delexicalizer
    * @param normalize    a token normalizer
    */
  def load(file: File,
           tokenize: String => TokenizedUtterance,
           delexicalize: String => String = defaultDelexicalize,
           normalize: TokenizedUtterance => TokenizedUtterance = Normalizer.identity): Dialogue = {
    // Extracting speaker and utterances
    val (utterances, line2speaker) = withSource(file) {
      source =>
        val line2speaker = mutable.ArrayBuffer[String]()
        val utterances = mutable.ArrayBuffer[Array[String]]()

        for (line <- source.getLines()) {
          val content = line.split(SEPARATOR_FIELD)
          assert(content.size <= 2, s"In dialogue ${file.getCanonicalPath}, " +
            s"the following line is not splittable: $line")

          // Dealing with the speaker
          val speakerStr = speakerNormalisation(content(0))
          line2speaker.append(speakerStr)

          // Dealing with the utterance
          val utterance =
            if (content.length <= 1) { // empty utterance case
              TokenizedUtterance.empty
            } else {
              /*
               * 1. the utterance is delexicalized
               * 2. the utterance is tokenized
               * 3. the utterance is normalized
               */
              normalize(tokenize(delexicalize(content(1))))
            }

          utterances.append(utterance)
        }

        (utterances.toIndexedSeq, line2speaker.toIndexedSeq)
    }

    // Building a valid name
    val filename = file.getAbsolutePath
    val name = filename.split("/").last.replaceAll("\\.", "_")

    Dialogue(name, utterances, line2speaker)
  }

  /**
    * Helper to normalize a string representing a speaker
    *
    */
  private def speakerNormalisation(speaker: String): String = {
    val result = speaker.trim
    if (result.size > 1 && result.last == ':') {
      result.init
    } else {
      result
    }
  }
}
