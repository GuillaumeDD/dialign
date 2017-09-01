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
package dialign.app

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import dialign.DialogueLexiconBuilder.ExpressionType
import dialign.{CSVUtils, DialogueLexiconBuilder, IO, Speaker}
import dialign.Speaker.Speaker
import dialign.metrics.online.{ActivationBasedMeasures, LexiconBasedMeasures, SelfRepetitionMeasures}
import dialign.nlp.Tokenizer.TokenizedUtterance

import scala.collection.mutable.ArrayBuffer

object OnlineMetricsComputerApp extends LazyLogging {

  case class Config(inputFile: File = new File("."),
                    withNormalisation: Boolean = false,
                    alpha: Double = 2.0d,
                    beta: Double = 2.0d,
                    lambda: Double = 0.5d)

  val parser = new scopt.OptionParser[Config]("dialign") {
    head("dialign", "2017.08")

    opt[File]('i', "input").required().valueName("<file>").
      action((x, c) => c.copy(inputFile = x)).
      text("input file containing dialogues")

    opt[Unit]('n', "normalisation").action((x, c) => c.copy(withNormalisation = true)).
      text("activates normalisation")

    opt[Double]('a', "alpha").valueName("<double>").action((x, c) => c.copy(alpha = x)).
      text("parameter of temporary activation")

    opt[Double]('b', "beta").valueName("<double>").action((x, c) => c.copy(beta = x)).
      text("parameter of permanent activation")

    opt[Double]('l', "lambda").valueName("<double>").action((x, c) => c.copy(lambda = x)).
      validate(lambda =>
        if (lambda < 0.0d || lambda > 1.0d) {
          failure("lambda should be comprised between 0.0 and 1.0 (inclusive)")
        } else {
          success
        }
      ).text("Relative importance of temporary activation against permanent activation. " +
      "The higher, the more important is the temporary activation and the less important is the permanent activation.")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // Reading dialogues
        import dialign.nlp.Tokenizer.{tokenizeWithoutMarkers => tokenize}
        import dialign.nlp.Normalizer._
        def preprocessTurn(s: String): TokenizedUtterance =
          if (config.withNormalisation) {
            tokenize(s).normalize
          } else {
            tokenize(s)
          }

        val dialogues = readFile(config.inputFile, preprocessTurn)

        // Exporting dialogues with measures
        val csvHeader = CSVUtils.mkCSV(List("Dial. Name", "Input line", "Index", "Num. Tokens", "Loc.", "Tokenized utt.",
          "SER",
          "ER",
          "Activation", "Inter-Repetition Dialogue structures", "Self-Repetition Dialogue structures"))

        def printDialogue(dialogue: PreprocessedDialogue): Unit = {
          val currentUtterances = ArrayBuffer.empty[TokenizedUtterance]
          val currentUtterancesFor = Map[Speaker, ArrayBuffer[TokenizedUtterance]](
            Speaker.A -> ArrayBuffer.empty[TokenizedUtterance],
            Speaker.B -> ArrayBuffer.empty[TokenizedUtterance])

          val utterances = dialogue.lines
          val turn2speaker = dialogue.turn2speaker _

          var currentNumTokens = 0

          for ((PreprocessedLine(line, utterance), index) <- utterances.zipWithIndex) {
            // Updating dialogue
            currentUtterances.append(utterance)

            val lastSpeaker = turn2speaker(index)
            for (speaker <- Speaker.values) {
              if (lastSpeaker == speaker) {
                currentUtterancesFor(speaker).append(utterance)
              } else {
                currentUtterancesFor(speaker).append(TokenizedUtterance.empty)
              }
            }

            // Computing metrics
            val selfRepetitionLexicon = DialogueLexiconBuilder(
              currentUtterancesFor(lastSpeaker),
              turn2speaker,
              ExpressionType.OWN_REPETITION_ONLY)

            val lexicon = DialogueLexiconBuilder(currentUtterances, turn2speaker)

            val lexiconMetrics = LexiconBasedMeasures(lexicon)
            val activationMetrics = ActivationBasedMeasures(
              lexicon = lexicon,
              alpha = config.alpha,
              beta = config.beta,
              lambda = config.lambda)
            val selfRepetitionMetrics = SelfRepetitionMeasures(selfRepetitionLexicon)

            val speaker = dialogue.turn2rawSpeaker(index)
            val interRepetitionDialogueStructures = activationMetrics.dialogueStructures.toSeq
              .sortBy(-_.content.size)
              .map(_.contentStr)
              .mkString(" | ")
            val selfRepetitionDialogueStructures = selfRepetitionMetrics.dialogueStructures.toSeq
              .sortBy(-_.content.size)
              .map(_.contentStr)
              .mkString(" | ")
            val csvLine = List(
              dialogue.name,
              line.replaceAll(CSVUtils.CSV_SEPARATOR, " "), // replacing column separator just in case
              index, currentNumTokens,
              speaker, utterance.mkString(" "),
              selfRepetitionMetrics.selfExpressionRepetition,
              lexiconMetrics.expressionRepetition,
              activationMetrics.activation(), interRepetitionDialogueStructures,
              selfRepetitionDialogueStructures)

            currentNumTokens += utterance.length

            println(CSVUtils.mkCSV(csvLine))
          }
        }

        for (dialogue <- dialogues) {
          println(csvHeader)
          printDialogue(dialogue)
          println()
        }

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }


  case class PreprocessedLine(rawLine: String, line: TokenizedUtterance)

  case class PreprocessedDialogue(name: String,
                                  lines: Array[PreprocessedLine],
                                  turn2rawSpeaker: Int => String) {

    val rawSpeakers =
      (for (i <- 0 until lines.length)
        yield (turn2rawSpeaker(i)))
        .toList
        .distinct
    assert(rawSpeakers.size <= 2, s"Dialogue with more than 2 speakers: ${rawSpeakers.mkString(", ")}")

    protected val rawSpeakers2Speaker = {
      if (rawSpeakers.isEmpty) {
        Map.empty[String, Speaker]
      } else if (rawSpeakers.size == 1) {
        Map(rawSpeakers.head -> Speaker.A)
      } else {
        Map(
          rawSpeakers.head -> Speaker.A,
          rawSpeakers.tail.head -> Speaker.B
        )
      }
    }

    def turn2speaker(index: Int): Speaker =
      rawSpeakers2Speaker(turn2rawSpeaker(index))
  }

  private def speakerNormalisation(speaker: String): String = {
    val result = speaker.trim
    if (result.size > 1 && result.last == ':') {
      result.init
    } else {
      result
    }
  }


  private def readFile(f: File,
                       preprocess: String => TokenizedUtterance): Array[PreprocessedDialogue] = {
    IO.withSource(f) {
      source =>
        val result = ArrayBuffer[PreprocessedDialogue]()
        var buffer = ArrayBuffer[PreprocessedLine]()
        var bufferSpeaker = ArrayBuffer[String]()

        val filename = f.getAbsolutePath
        val name = filename.split("/").last.replaceAll("\\.", "_")

        for {
          line <- source.getLines()
        } {
          if (line.trim.isEmpty) { // case: empty line
            // Addition of the buffer if it is non-empty
            if (buffer.nonEmpty) {
              result.append(PreprocessedDialogue(name, buffer.toArray, bufferSpeaker))
            }
            // Re-initialisation of the buffers
            buffer = ArrayBuffer[PreprocessedLine]()
            bufferSpeaker = ArrayBuffer[String]()

          } else {
            // case: non-empty line

            // Reading the line
            val content = line.split("\t")
            assert(content.size <= 2, s"Line is not splittable: $line")
            // content(0) is the speaker
            val speaker = speakerNormalisation(content(0))
            bufferSpeaker.append(speaker)

            // surface form of the utterance
            val utterance = if(content.size > 1){
              content(1)
            } else {
              ""
            }

            buffer.append(PreprocessedLine(line, preprocess(utterance)))
          }
        }
        // Adding the last dialogue of the file
        if (buffer.nonEmpty) {
          result.append(PreprocessedDialogue(name, buffer.toArray, bufferSpeaker))
        }

        result.toArray
    }
  }
}
