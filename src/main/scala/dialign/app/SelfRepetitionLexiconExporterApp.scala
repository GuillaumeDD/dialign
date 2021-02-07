/*
 * Copyright ISIR, CNRS
 *
 * Contributor(s) :
 *    Guillaume Dubuisson Duplessis <guillaume@dubuissonduplessis.fr> (2017, 2020, 2021)
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
import dialign._
import dialign.IO.DialogueReader
import dialign.nlp.Tokenizer.TokenizedUtterance
import dialign.nlp.{Normalizer, Tokenizer}

/**
  * Computes and exports a corpus lexicon of self-repetitions from a corpus of dialogue transcripts
  *
  */
object SelfRepetitionLexiconExporterApp extends LazyLogging {

  case class Config(inputDirectory: File = new File("."),
                    outputFilenamePrefix: String = "corpus-lexicon-self-repetition",
                    withNormalisation: Boolean = false,
                    withBeginAndEndMarkers: Boolean = false,
                    filenamePrefix: String = "",
                    filenameSuffix: String = "",
                    filenameExtension: String = "txt"
                   )

  val parser = new scopt.OptionParser[Config]("dialignSR") {
    head("dialignSR", "2017.11")

    opt[File]('i', "input").required().valueName("<directory>").
      validate(dirname =>
        if (dirname.isDirectory) success
        else failure("Input must be a directory!")).
      action((x, c) => c.copy(inputDirectory = x)).
      text("input directory containing dialogue files")

    opt[String]('o', "output").optional().valueName("<filename_prefix>").
      validate(filenamePrefix =>
        if (filenamePrefix.trim.nonEmpty) success
        else failure("Output filename prefix should not be empty or full of spaces")).
      action((x, c) => c.copy(outputFilenamePrefix = x)).
      text("output filename prefix for computed lexicon files")

    opt[Unit]('n', "normalisation").action((_, c) => c.copy(withNormalisation = true)).
      text("activates token normalisation")

    opt[Unit]('m', "markers").action((_, c) => c.copy(withBeginAndEndMarkers = true)).
      text("adds begin and end markers to utterances")

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
        val inputFiles = IO.getFilenames(config.inputDirectory,
          config.filenamePrefix, config.filenameSuffix, config.filenameExtension)

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

          val tokenize = if (config.withBeginAndEndMarkers) {
            Tokenizer.tokenize _
          } else {
            Tokenizer.tokenizeWithoutMarkers _
          }

          // Loading dialogues
          val dialogues = for (file <- inputFiles)
            yield DialogueReader.load(file = file,
              tokenize = tokenize,
              normalize = normalize)

          logger.debug(s"${dialogues.size} dialogue files have been loaded")

          val dialogueLexicons =
            for (dialogue <- dialogues)
              yield {
                logger.debug(s"Building dialogue lexicon for dialogue: ${dialogue.name}")
                DialogueProcessor(dialogue)
              }

          val dialogueLexiconsForA = dialogueLexicons.map(_.lexiconForA)
          val dialogueLexiconsForB = dialogueLexicons.map(_.lexiconForB)

          // Outputing the results
          def outputLexicon(lexicons: Seq[DialogueLexicon], speakerName: String): Unit = {
            val corpusLexicon = CorpusLexicon.aggregate(lexicons)
            val filename = s"${config.outputFilenamePrefix}-$speakerName.csv"
            logger.debug(s"Outputing lexicon for $speakerName in file: $filename")
            IO.withFile(filename) {
              writer =>
                writer.println(corpusLexicon.mkHierarchicalInventory)
            }
          }

          outputLexicon(dialogueLexiconsForA, "A")
          outputLexicon(dialogueLexiconsForB, "B")
        }

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }

  protected case class DialogueProcessor(dialogue: DialogueReader.Dialogue) {

    val dialogueID = dialogue.name
    val utterances = dialogue.utterances
    val turnID2Speaker = dialogue.getSpeaker _
    val speaker2str = dialogue.getRawSpeaker _

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
    logger.debug(s"Building dialogue lexicons for dialogue: $dialogueID")
    // Self-repetition lexicon for speaker A
    val lexiconForA = DialogueLexiconBuilder(
      speaker2utterances(Speaker.A),
      turnID2Speaker, speaker2str,
      ExpressionType.OWN_REPETITION_ONLY)
    // Self-repetition lexicon for speaker B
    val lexiconForB = DialogueLexiconBuilder(
      speaker2utterances(Speaker.B),
      turnID2Speaker, speaker2str,
      ExpressionType.OWN_REPETITION_ONLY)
  }

}
