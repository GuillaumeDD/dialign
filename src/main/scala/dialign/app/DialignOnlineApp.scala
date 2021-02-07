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
import dialign.IO.DialogueReader.Dialogue
import dialign.online.{DialogueHistory, Utterance, UtteranceScoring}
import dialign.{CSVUtils, Expression}

import scala.Console.{BOLD, GREEN, RED, RESET}
import scala.io.StdIn

object DialignOnlineApp extends LazyLogging {

  case class Config(history: Option[File] = None,
                    output: Option[File] = None,
                    verbose: Boolean = false)

  val parser = new scopt.OptionParser[Config]("dialign-online") {
    head("dialign-online", "1.0")
    note("This software is governed by the CeCILL-B license under French law and\n" +
      "abiding by the rules of distribution of free software.  You can use, \n" +
      "modify and/or redistribute the software under the terms of the CeCILL-B\n" +
      "license as circulated by CEA, CNRS and INRIA at the following URL\n" +
      "\"http://www.cecill.info\".\n" +
      "See also: https://github.com/GuillaumeDD/dialign\n")

    opt[File]('f', "file").optional().valueName("<file>").
      validate(filename =>
        if (filename.isFile)
          success
        else
          failure("History must be a file!")).
      action((x, c) => c.copy(history = Some(x))).
      text("dialogue history file")

    opt[File]('o', "output").optional().valueName("<file>").
      validate(filename =>
        if (filename.exists()) {
          if (filename.canWrite){
            success
          } else {
            failure("Output file exists but cannot be written. Check the write permission.")
          }
        } else {
          success
        }
      ).
      action((x, c) => c.copy(output = Some(x))).
      text("output file to export online measures (deactivate the interactive mode)")

    opt[Unit]('v', "verbose").action((_, c) => c.copy(verbose = true)).
      text("display logs on console")

  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) =>
        // Building dialogue history from utterance
        val utterances = config.history match {
          case None =>
            IndexedSeq.empty[Utterance]
          case Some(file) =>
            IO.loadFile(file)
        }

        val dialogueHistory = DialogueHistory(utterances)

        config.output match {
          case None =>
            // Running the iteractive loop
            interactiveRun(dialogueHistory)

          case Some(outputFile) =>
            IO.exportDialogueHistory(dialogueHistory, outputFile, verbose = config.verbose)
        }

      case None =>
      // arguments are bad, error message will have been displayed

    }
  }

  def interactiveRun(dialogueHistory: DialogueHistory): Unit = {
    /*
     * INITIALISATION
     */
    val currentLocutors = scala.collection.mutable.Buffer[String]()

    def updateLocutor(loc: String): Unit = {
      // Update locutor if needed
      if ((currentLocutors.size < 2) && !currentLocutors.contains(loc)) {
        currentLocutors.addOne(loc)
      }
    }

    def isValidLocutor(loc: String): Boolean = {
      if (currentLocutors.size < 2) {
        loc.trim.nonEmpty
      } else {
        loc.trim.nonEmpty && currentLocutors.contains(loc)
      }
    }

    for (Utterance(locutor, _) <- dialogueHistory.history()) {
      updateLocutor(locutor)
    }

    def addUtterance(utterance: Utterance): Unit = {
      updateLocutor(utterance.locutor)

      dialogueHistory.addUtterance(utterance)

      IO.printDialogueHistory(dialogueHistory)
    }

    /*
     * INTERACTIVE LOOP
     */

    // Printing the dialogue history
    IO.printDialogueHistory(dialogueHistory)

    var running = true
    var m = '?'
    do {
      m = IO.readValidMode()

      m match {
        case 'q' =>
          running = false

        case 'h' =>
          IO.printHelpMessage()

        case 'e' =>
          val file = IO.readFilepath()
          IO.exportDialogueHistory(dialogueHistory, file)

        case 's' =>
          // Obtain utterance
          val utterance = IO.readUtterance(isValidLocutor, currentLocutors.toSeq)
          // Score utterance and print results
          val result = dialogueHistory.score(utterance)
          IO.printScoringResult(result)

          // Ask a y/n question if utterance should be added
          val addUtteranceNext = IO.readYN("Add the utterance to dialogue history?", defaultYes = false)

          // Add utterance if needed and print dialogue history
          if(addUtteranceNext) addUtterance(utterance)

        case 'p' =>
          IO.printDialogueHistory(dialogueHistory)

        case 'a' =>
          val utterance = IO.readUtterance(isValidLocutor, currentLocutors.toSeq)
          addUtterance(utterance)
      }

    } while (running)
  }
}

object IO {
  def isValidMode(m: Char): Boolean =
    (m == 'h') || (m == 's') || (m == 'a') || (m == 'q') || (m == 'p') || (m == 'e')

  def readValidMode(): Char = {
    var m = '?'

    do {
      Console.println(s"${RESET}${BOLD}${RED}Mode?${RESET} (h for help, s, a, p, e, q)")

      try {
        m = StdIn.readChar().toLower
      } catch {
        case _: java.lang.StringIndexOutOfBoundsException =>
          m = '?'
        case _: java.io.EOFException =>
          m = '?'
      }
    } while (!isValidMode(m))

    m
  }

  def readFilepath(): File = {
    def isValidFile(file: File): Boolean = {
      if(file.exists() && file.isFile && file.canWrite){
        IO.readYN(f"Overwrite existing file? (${file.getCanonicalFile})", defaultYes = false)
      } else {
        true
      }
    }

    var file = new java.io.File(".")

    do {
      Console.println(s"${RESET}${BOLD}${GREEN}Filepath?${RESET}")
      val filename = StdIn.readLine()
      file = new File(filename)
    } while(! isValidFile(file))

    file
  }

  def readUtterance(isValidLocutor: String => Boolean,
                    availableLocutors: Seq[String]): Utterance = {
    var locutor = ""

    do {
      val locutorsStr = availableLocutors.map(
        locutor => s"'${locutor}'"
      ).mkString(",")

      if (availableLocutors.size > 0 && availableLocutors.size < 2) {
        Console.println(s"${RESET}${BOLD}${RED}Locutor?${RESET} (existing: ${locutorsStr})")
      } else if (availableLocutors.size == 2) {
        Console.println(s"${RESET}${BOLD}${RED}Locutor?${RESET} (possible values: ${locutorsStr})")
      } else {
        Console.println(s"${RESET}${BOLD}${RED}Locutor?${RESET}")
      }

      locutor = StdIn.readLine()
    } while (!isValidLocutor(locutor))

    var utterance = ""
    Console.println(s"${RESET}${BOLD}${GREEN}Utterance?${RESET}")
    utterance = StdIn.readLine()

    Utterance(locutor, utterance)
  }

  def readYN(message : String, defaultYes: Boolean = true): Boolean = {
    val msgYN = if(defaultYes) "[y]/n" else "y/[n]"

    var answer = '?'

    do {
      Console.println(s"${RESET}${BOLD}${GREEN}${message}${RESET} (${msgYN})")
      try {
        answer = StdIn.readChar().toLower
      } catch {
        case _: java.lang.StringIndexOutOfBoundsException =>
          answer = if(defaultYes) 'y' else 'n'
        case _: java.io.EOFException =>
          answer = if(defaultYes) 'y' else 'n'
      }
    } while(answer != 'y' && answer != 'n')

    answer == 'y'
  }

  def printHelpMessage(): Unit = {
    Console.println(
      """Enter:
        | h for help
        | s to score a new utterance
        | a to add a new utterance
        | p to print dialogue history
        | e to export dialogue
        | q to quit
      """.stripMargin('|'))
  }

  def printDialogueHistory(dialogueHistory: DialogueHistory): Unit = {
    val locutorToColor =
      (for ((locutor, index) <- dialogueHistory.locutors().zipWithIndex)
        yield (locutor, (if (index % 2 == 0) Console.BLUE else Console.GREEN))).toMap


    for ((Utterance(locutor, utterance), index) <- dialogueHistory.history().zipWithIndex) {
      val color = locutorToColor(locutor)
      Console.println(f"${RESET}${BOLD}${color}$locutor%8s|$index%02d${RESET}: ${utterance}${RESET}")
    }
  }

  def printScoringResult(result: UtteranceScoring): Unit = {
    def expressionToString(expr: Expression): String =
      f"'${expr.content.mkString(" ")}'"

    val Utterance(locutor, text) = result.utterance
    val der = result.der
    val dser = result.dser
    Console.println(
      f"""Utterance:
         |${RESET}${BOLD}${Console.RED}$locutor${RESET}: ${text}${RESET}""".stripMargin)
    Console.println(
      f"""Verbal alignment
         | - DER: $der%.2f
         | - Reused shared lexical patterns: ${result.sharedExpressions.map(expressionToString).mkString(", ")}
         | - Established shared lexical patterns: ${result.establishedSharedExpressions.map(expressionToString).mkString(", ")}""".stripMargin)
    Console.println(
      f"""Self-repetitions
         | - DSER: $dser%.2f
         | - Reused self-repetition patterns: ${result.selfExpressions.map(expressionToString).mkString(", ")}
         |""".stripMargin)
  }

  def exportDialogueHistory(history: DialogueHistory, file: File, verbose: Boolean = true): Unit = {
    def expressionToString(expr: Expression): String =
      f"${expr.content.mkString(" ")}"

    if(verbose)
      Console.println(f"Exporting history in: ${file.getAbsolutePath}")

    // Incrementally built history of the export
    val exportHistory = DialogueHistory()

    dialign.IO.withFile(file) {
      writer =>
        val heading = List(
          "locutor", "utterance",
          "der", "sharedExpressions", "establishedSharedExpressions",
          "dser", "selfRepetitions"
        )
        writer.println(CSVUtils.mkCSV(heading))

        for(u @ Utterance(locutor, text) <- history.history()){
          // Scoring of the new utterance
          val result = exportHistory.score(u)

          // Saving the result of the scoring
          val newLine = List(
            "\"" + CSVUtils.csvProtect(locutor) + "\"",
            "\"" + CSVUtils.csvProtect(text) + "\"",
            // Verbal alignment
            f"${result.der}",
            "\"" + CSVUtils.csvProtect(result.sharedExpressions.map(expressionToString).mkString("\n")) + "\"",
            "\"" + CSVUtils.csvProtect(result.establishedSharedExpressions.map(expressionToString).mkString("\n")) + "\"",
            // Self-repetitions
            f"${result.dser}",
            "\"" + CSVUtils.csvProtect(result.selfExpressions.map(expressionToString).mkString("\n")) + "\""
          )
          writer.println(CSVUtils.mkCSV(newLine))

          // Updating the history
          exportHistory.addUtterance(u)
        }
    }
  }

  def loadFile(file: File): IndexedSeq[Utterance] = {
    def dialogueToUtterances(dialogue: Dialogue): IndexedSeq[Utterance] = {
      for((utterance, index) <- dialogue.utterances.zipWithIndex)
        yield(Utterance(dialogue.turn2rawSpeaker(index), utterance.mkString(" ")))
    }

    val dialogue = dialign.IO.DialogueReader.load(file,  tokenize = dialign.nlp.Tokenizer.tokenizeWithoutMarkers)

    dialogueToUtterances(dialogue)
  }
}
