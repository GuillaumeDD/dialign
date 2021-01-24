package dialign.online

import Console.{BOLD, GREEN, RED, RESET, YELLOW_B}
import java.io.File

import com.typesafe.scalalogging.LazyLogging

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

  def isValidMode(m: Char): Boolean =
    (m == 'h') || (m == 's') || (m == 'a') || (m == 'q') || (m == 'p') || (m == 'e')

  def readValidMode(): Char = {
    var m = '?'

    do {
      Console.println(s"${RESET}${BOLD}${RED}Mode?${RESET} (h for help, s, a, p, q)")
      m = StdIn.readChar().toLower
    } while (!isValidMode(m))

    m
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

      printDialogueHistory(dialogueHistory)
    }

    /*
     * INTERACTIVE LOOP
     */

    // Printing the dialogue history
    printDialogueHistory(dialogueHistory)

    var running = true
    var m = '?'
    do {
      m = readValidMode()

      m match {
        case 'q' =>
          running = false

        case 'h' =>
          printHelpMessage()

        case 'e' =>
        // TODO: export dialogue

        case 's' =>
          // Obtain utterance
          val utterance = readUtterance(isValidLocutor, currentLocutors.toSeq)
          // Score utterance and print results
          val result = dialogueHistory.score(utterance)
          Console.println(result)

        // TODO: ask a y/n question if utterance should be added

        // TODO: add utterance if needed and print dialogue history

        case 'p' =>
          printDialogueHistory(dialogueHistory)

        case 'a' =>
          val utterance = readUtterance(isValidLocutor, currentLocutors.toSeq)
          addUtterance(utterance)
      }

    } while (running)
  }
}
