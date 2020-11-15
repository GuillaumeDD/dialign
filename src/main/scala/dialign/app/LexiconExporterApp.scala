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

import dialign.IO.{getFilenames, withFile}
import com.typesafe.scalalogging.LazyLogging
import dialign.DialogueLexiconBuilder.ExpressionType
import dialign.{CorpusLexicon, DialogueLexiconBuilder}
import dialign.IO.DialogueReader
import dialign.IO.DialogueReader.Dialogue
import dialign.nlp.{Delexicalizer, Normalizer, Tokenizer}

/**
  * Computes and export a shared lexicon from a corpus of dialogue transcripts
  *
  */
object LexiconExporterApp extends LazyLogging {

  case class Config(inputDirectory: File = new File("."),
                    outputFile: File = new File("lexicon-RSTP.csv"),
                    withDelexicalisation: Boolean = false,
                    withNormalisation: Boolean = false,
                    withBeginAndEndMarkers: Boolean = false,
                    rstpType: String = "all",
                    filenamePrefix: String = "",
                    filenameSuffix: String = "",
                    filenameExtension: String = "txt"
                   )

  val parser = new scopt.OptionParser[Config]("dialRSTP") {
    head("dialRSTP", "2017.07")

    opt[File]('i', "input").required().valueName("<directory>").
      validate(dirname =>
        if (dirname.isDirectory) success
        else failure("Input must be a directory!")).
      action((x, c) => c.copy(inputDirectory = x)).
      text("input directory containing dialogue files")

    opt[File]('o', "output").optional().valueName("<filename>").
      action((x, c) => c.copy(outputFile = x)).
      text("output filename for the lexicon of RSTP")

    opt[String]('r', "rstp").action((x, c) => c.copy(rstpType = x)).
      validate(rstpType =>
        if (rstpType == "all" || rstpType == "intra" || rstpType == "inter") success
        else failure(s"RSTP type must be one of this value: all|intra|inter (here: '$rstpType')")).
      text("RSTP to consider: all|intra|inter")

    opt[Unit]('d', "delexicalisation").action((_, c) => c.copy(withDelexicalisation = true)).
      text("activates delexicalisation")

    opt[Unit]('n', "normalisation").action((_, c) => c.copy(withNormalisation = true)).
      text("activates normalisation")

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
        val inputFiles = getFilenames(config.inputDirectory,
          config.filenamePrefix, config.filenameSuffix, config.filenameExtension)

        if (inputFiles.isEmpty) {
          println(s"No dialogue file found in directory '${config.inputDirectory.getAbsolutePath}'" +
            s" with required prefix ='${config.filenamePrefix}'," +
            s" required suffix='${config.filenameSuffix}'" +
            s" and required extension='${config.filenameExtension}'")

        } else {
          logger.debug("Loading dialogue files" +
            s" from directory '${config.inputDirectory.getAbsolutePath}'" +
            s" with required prefix ='${config.filenamePrefix}'," +
            s" required suffix='${config.filenameSuffix}'" +
            s" and required extension='${config.filenameExtension}'")

          // Determining the normalizer to use
          val normalize = if (config.withNormalisation) {
            Normalizer.normalize _
          } else {
            Normalizer.identity _
          }

          // Determining the delexicaliser to use
          val delexicalize = if (config.withDelexicalisation) {
            Delexicalizer.delexicalize _
          } else {
            Delexicalizer.identity _
          }

          val tokenize = if(config.withBeginAndEndMarkers){
            Tokenizer.tokenize _
          } else {
            Tokenizer.tokenizeWithoutMarkers _
          }

          val dialogues = for (file <- inputFiles)
            yield DialogueReader.load(file, tokenize,
              delexicalize,
              normalize)

          logger.debug(s"${dialogues.size} dialogue files have been loaded")

          val lexiconMode =
            if (config.rstpType == "all") {
              ExpressionType.ALL
            } else if (config.rstpType == "intra") {
              ExpressionType.OWN_REPETITION_ONLY
            } else {
              // config.rstpType == "inter"
              ExpressionType.INTER_REPETITION_ONLY
            }

          val dialogueLexicons =
            for (d@Dialogue(dialogueName, utterances, getRawSpeaker) <- dialogues)
              yield {
                logger.debug(s"Building dialogue lexicon for dialogue: $dialogueName")
                DialogueLexiconBuilder(utterances, d.getSpeaker, d.getRawSpeaker, lexiconMode)
              }

          val corpusLexicon = CorpusLexicon.aggregate(dialogueLexicons)

          logger.debug(s"Outputing lexicon in file: ${config.outputFile}")
          withFile(config.outputFile) {
            writer =>
              writer.println(corpusLexicon.mkHierarchicalInventory)
          }
        }

      case None =>
      // arguments are bad, error message will have been displayed

    }
  }
}
