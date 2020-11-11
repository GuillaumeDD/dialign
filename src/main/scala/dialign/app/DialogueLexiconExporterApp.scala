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
import dialign.{CSVUtils, DialogueLexiconBuilder, IO, Speaker}
import dialign.metrics.offline.DialogueLexiconMeasures
import dialign.metrics.offline.DialogueLexiconMeasures.{toCSV, toCSVSelfRepetition}
import dialign.IO.{DialogueReader, getFilenames}
import dialign.DialogueLexicon.{mkHierarchicalInventory, mkSelfRepetitionHierarchicalInventory, mkStringTurns}
import dialign.DialogueLexiconBuilder.ExpressionType
import dialign.nlp.Tokenizer.TokenizedUtterance
import dialign.nlp.{Normalizer, Tokenizer}

/**
  * Application that computes verbal alignment measures for a set of dialogue files
  *
  * It takes as input a set of dialogue files (i.e. dialogue transcripts) and outputs:
  *  - for each dialogue, the expression lexicon and a tagged version of the dialogue transcript with expressions
  *  - for the whole corpus, a synthesis file with the computed measures for each dialogue
  *
  */
object DialogueLexiconExporterApp extends LazyLogging {

  case class Config(inputDirectory: File = new File("."),
                    outputDirectory: File = new File("."),
                    outputSynthesisFilename: String = "dial-synthesis.csv",
                    withNormalisation: Boolean = false,
                    filenamePrefix: String = "",
                    filenameSuffix: String = "",
                    filenameExtension: String = "txt"
                   )

  val parser = new scopt.OptionParser[Config]("dialign") {
    head("dialign", "2017.08")

    opt[File]('i', "input").required().valueName("<directory|file>").
      validate(dirname =>
        if (dirname.isDirectory || dirname.isFile) success
        else failure("Input must be a directory or a file!")).
      action((x, c) => c.copy(inputDirectory = x)).
      text("single input file or an input directory containing dialogue files")

    opt[File]('o', "output").optional().valueName("<directory>").
      validate(dirname =>
        if (dirname.exists()) {
          // Output directory exists
          if (dirname.isDirectory) {
            if (dirname.canWrite) {
              success
            } else{
              failure(s"Cannot write in output directory: $dirname")
            }
          } else {
            failure(s"It seems that '$dirname' is not a writeable directory. Output must be a directory.")
          }
        } else {
          // Output directory does not exists
          if(dirname.mkdir()) {
            success
          } else {
            failure(s"Cannot create output directory: $dirname")
          }
        }).
      action((x, c) => c.copy(outputDirectory = x)).
      text("output directory for the computed dialogue lexicon files")

    opt[String]('y', "synthesis").optional().valueName("<filename_synthesis>").
      action((x, c) => c.copy(outputSynthesisFilename = x)).
      text("output filename for the synthesis file regrouping measures on all the dialogues")

    opt[Unit]('n', "normalisation").action((_, c) => c.copy(withNormalisation = true)).
      text("activates token normalisation")

    opt[String]('p', "prefix").optional().valueName("<filename_prefix>").
      action((x, c) => c.copy(filenamePrefix = x)).
      text("required prefix of the dialogue file names")

    opt[String]('s', "suffix").optional().valueName("<filename_suffix>").
      action((x, c) => c.copy(filenameSuffix = x)).
      text("required suffix of the dialogue file names")

    opt[String]('e', "extension").optional().valueName("<filename_extension>").
      action((x, c) => c.copy(filenameExtension = x)).
      text("required extension of the dialogue file names (without the '.'; e.g. 'txt')")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // Loading files
        val inputFiles =
          if(config.inputDirectory.isDirectory) {
            // Case: directory
            getFilenames(config.inputDirectory,
              config.filenamePrefix, config.filenameSuffix, config.filenameExtension)
          } else if(config.inputDirectory.isFile) {
            // Case: single file
            List(config.inputDirectory)
          } else {
            // Case: unexpected
            List.empty[File]
          }

        if (inputFiles.isEmpty) {
          println(s"No dialogue file found in directory '${config.inputDirectory.getAbsolutePath}'" +
            s" with required prefix ='${config.filenamePrefix}'," +
            s" required suffix='${config.filenameSuffix}'" +
            s" and required extension='${config.filenameExtension}'")

        } else {
          // Determining the normalizer to use
          val normalize = if (config.withNormalisation) {
            Normalizer.normalize _
          } else {
            Normalizer.identity _
          }

          // Loading dialogues
          val dialogues = for (file <- inputFiles)
            yield DialogueReader.load(file = file,
              tokenize = Tokenizer.tokenizeWithoutMarkers,
              normalize = normalize)

          logger.debug(s"${dialogues.size} dialogue files have been loaded")

          // Outputing the results
          val OUTPUT_DIR = config.outputDirectory.getAbsolutePath

          val results = for (dialogue <- dialogues)
            yield {
              val result = DialogueProcessor(OUTPUT_DIR, dialogue).process()
              (dialogue.name, result)
            }

          // Outputing the synthesis regrouping a synthesis of all the files
          val PATH_FILENAME_OUTPUT = s"$OUTPUT_DIR/${config.outputSynthesisFilename}"
          logger.debug(s"Outputing dialogue synthesis in $PATH_FILENAME_OUTPUT")
          IO.withFile(PATH_FILENAME_OUTPUT) {
            writer =>
              writer.println(DialogueLexiconMeasures.headingToCSV)
              for ((name, result) <- results) {
                writer.println(CSVUtils.join(name, result))
              }
          }
        }

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }

  case class DialogueProcessor(outputDir: String,
                               dialogue: DialogueReader.Dialogue) {

    val dialogueID = dialogue.name
    val utterances = dialogue.utterances
    val turnID2Speaker = dialogue.getSpeaker _

    /*
     * Mapping between a speaker and the sequence of utterances that forms the dialogue but with the utterance
     * from the other speaker replaced by an empty utterances
     */
    val speaker2utterances = (
      for (speaker <- Speaker.values.toSeq)
        yield ({
          // Keeping the sequence of utterances intact but replacing utterances from the
          // other speaker by empty utterances
          val utterancesFromSpeaker =
          for ((utt, index) <- utterances.zipWithIndex)
            yield (if (turnID2Speaker(index) == speaker) {
              utt
            } else {
              TokenizedUtterance.empty
            })
          (speaker -> utterancesFromSpeaker)
        })
      ).toMap

    // Building the lexicon
    logger.debug(s"Building dialogue lexicon for dialogue: $dialogueID")
    // Other-repetition lexicon
    val lexicon = DialogueLexiconBuilder(utterances, turnID2Speaker)
    // Self-repetition lexicon for speaker A
    val lexiconForA = DialogueLexiconBuilder(
      speaker2utterances(Speaker.A),
      turnID2Speaker,
      ExpressionType.OWN_REPETITION_ONLY)
    // Self-repetition lexicon for speaker B
    val lexiconForB = DialogueLexiconBuilder(
      speaker2utterances(Speaker.B),
      turnID2Speaker,
      ExpressionType.OWN_REPETITION_ONLY)

    def process(): String = {
      outputLexicon()
      outputSelfRepetitionLexicon()
      outputDialogue()

      val otherRepetitionMeasures = toCSV(DialogueLexiconMeasures(lexicon))
      val selfRepetitionA = toCSVSelfRepetition(DialogueLexiconMeasures(lexiconForA))
      val selfRepetitionB = toCSVSelfRepetition(DialogueLexiconMeasures(lexiconForB))

      CSVUtils.join(otherRepetitionMeasures, CSVUtils.join(selfRepetitionA, selfRepetitionB))
    }


    def outputDialogue(): Unit = {
      val filename_dialogueTXT = s"$outputDir/$dialogueID-dialogue.txt"
      logger.debug(s"Outputing dialogue in $filename_dialogueTXT (for dialogue: $dialogueID)")

      IO.withFile(filename_dialogueTXT) {
        writer =>
          writer.println(mkStringTurns(lexicon))

          writer.println()

          writer.println("TURN DETAILS:")
          for (turn <- lexicon.turns) {
            writer.println(turn)
          }
      }
    }

    def outputLexicon(): Unit = {
      val filename_lexicon = s"$outputDir/$dialogueID-lexicon.csv"
      logger.debug(s"Outputing dialogue lexicon in $filename_lexicon (for dialogue: $dialogueID)")

      IO.withFile(filename_lexicon) {
        writer =>
          writer.println(mkHierarchicalInventory(lexicon))
      }
    }

    def outputSelfRepetitionLexicon(): Unit = {
      // Self-repetition lexicon for A
      val filename_lexicon_A = s"$outputDir/$dialogueID-lexicon-self-rep-A.csv"
      logger.debug(s"Outputing self-repetition dialogue lexicon for speaker A in $filename_lexicon_A (for dialogue: $dialogueID)")

      IO.withFile(filename_lexicon_A) {
        writer =>
          writer.println(mkSelfRepetitionHierarchicalInventory(lexiconForA))
      }

      // Self-repetition lexicon for B
      val filename_lexicon_B = s"$outputDir/$dialogueID-lexicon-self-rep-B.csv"
      logger.debug(s"Outputing self-repetition dialogue lexicon for speaker B in $filename_lexicon_B (for dialogue: $dialogueID)")

      IO.withFile(filename_lexicon_B) {
        writer =>
          writer.println(mkSelfRepetitionHierarchicalInventory(lexiconForB))
      }
    }
  }

}
