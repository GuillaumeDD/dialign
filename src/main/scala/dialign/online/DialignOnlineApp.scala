package dialign.online

import Console.{BOLD, GREEN, RED, RESET, YELLOW_B}
import java.io.File

import com.typesafe.scalalogging.LazyLogging
import dialign.{CSVUtils, Expression}

import scala.io.StdIn

object DialignOnlineApp extends LazyLogging {

  case class Config(history: Option[File] = None,
                    verbose: Boolean = false
                   )

  val parser = new scopt.OptionParser[Config]("dialign-online") {
    head("dialign-online", "2021.01")

    opt[File]('h', "history").optional().valueName("<file>").
      validate(filename =>
        if (filename.isFile)
          success
        else
          failure("History must be a file!")).
      action((x, c) => c.copy(history = Some(x))).
      text("dialogue history file")

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
            // TODO
            IndexedSeq.empty[Utterance]
        }

        val dialogueHistory = DialogueHistory(utterances)

        // Running the iteractive loop
        interactiveRun(dialogueHistory)

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
        // TODO: export dialogue

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
        case e: java.lang.StringIndexOutOfBoundsException =>
          m = '?'
        case e: java.io.EOFException =>
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
        case e: java.lang.StringIndexOutOfBoundsException =>
          answer = if(defaultYes) 'y' else 'n'
        case e: java.io.EOFException =>
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

    val Utterance(locutor, utterance) = result.utterance
    val der = result.der
    val dser = result.dser
    Console.println(
      f"""Utterance:
         |${RESET}${BOLD}${Console.RED}$locutor${RESET}: ${utterance}${RESET}""".stripMargin)
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

  def exportDialogueHistory(history: DialogueHistory, file: File): Unit = {
    def expressionToString(expr: Expression): String =
      f"${expr.content.mkString(" ")}"

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

        for(u @ Utterance(locutor, utterance) <- history.history()){
          // Scoring of the new utterance
          val result = exportHistory.score(u)

          // Saving the result of the scoring
          val newLine = List(
            locutor,
            utterance,
            // Verbal alignment
            f"${result.der}",
            result.sharedExpressions.map(expressionToString).mkString("\n"),
            result.establishedSharedExpressions.map(expressionToString).mkString("\n"),
            // Self-repetitions
            f"${result.dser}",
            result.selfExpressions.map(expressionToString).mkString("\n")
          )
          writer.println(CSVUtils.mkCSV(newLine))

          // Updating the history
          exportHistory.addUtterance(u)
        }
    }
  }
}
