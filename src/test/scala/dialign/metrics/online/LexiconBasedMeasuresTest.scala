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

import dialign.DialogueLexiconBuilder
import org.scalatest.funsuite.AnyFunSuite


class LexiconBasedMeasuresTest extends AnyFunSuite {

  trait Dialogues {

    import dialign.Speaker.{A, B}
    import dialign.nlp.Tokenizer.{tokenizeWithoutMarkers => tokenize}

    val utterances1 = IndexedSeq(
      tokenize("so what did you and the other animals do next ?"),
      tokenize("the other animals and I swam to the shore , get out of the water and we were all wet .")
    )
    val turn2speaker1 = Map(0 -> A, 1 -> B)
    val lexicon1 = DialogueLexiconBuilder(utterances1, turn2speaker1)


    val utterances2 = IndexedSeq(
      tokenize("foo"),
      tokenize("bar")
    )
    val turn2speaker2 = Map(0 -> A, 1 -> B)
    val lexicon2 = DialogueLexiconBuilder(utterances2, turn2speaker2)
  }

  test("Expression repetition online metrics should compute the ER of the last utterance") {
    new Dialogues {
      // Test with dialogue 1
      val measures1 = LexiconBasedMeasures(lexicon1)
      assert(measures1.expressionRepetition == 7.0d / 21.0d)

      // Test with dialogue 2
      val measures2 = LexiconBasedMeasures(lexicon2)
      assert(measures2.expressionRepetition == 0.0d)
    }
  }

  test("Expression contribution online metrics should compute the number of established expression of the last turn") {
    new Dialogues {
      // Test with dialogue 1
      val measures1 = LexiconBasedMeasures(lexicon1)
      assert(measures1.lexiconContribution == 3)

      // Test with dialogue 2
      val measures2 = LexiconBasedMeasures(lexicon2)
      assert(measures2.lexiconContribution == 0)
    }
  }
}
