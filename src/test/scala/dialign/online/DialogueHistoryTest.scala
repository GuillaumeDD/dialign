package dialign.online

import dialign.Expression
import org.scalatest.funsuite.AnyFunSuite

class DialogueHistoryTest  extends AnyFunSuite {

  test("Empty dialogue history should appropriately score a new utterance") {
    val dialogueHistory = DialogueHistory()
    val newUtterance = Utterance("Alice", "Hello world !")

    val result = dialogueHistory.score(newUtterance)

    val expectedResult = UtteranceScoring(
      utterance = Utterance("Alice", "Hello world !"),
      der = 0.0d,
      dser = 0.0d,
      sharedExpressions = Set.empty,
      establishedSharedExpressions = Set.empty,
      selfExpressions = Set.empty)

    assert(result == expectedResult)
  }

  test("Empty dialogue history should add a new utterance") {
    val dialogueHistory = DialogueHistory()
    val newUtterance = Utterance("Alice", "Hello world !")

    // An empty dialogue history should be empty
    val result0 = dialogueHistory.history().length
    val expectedResult0 = 0
    assert(result0 == expectedResult0)

    dialogueHistory.addUtterance(newUtterance)

    // The new utterance should be added
    val result1 = dialogueHistory.history().length
    val expectedResult1 = 1
    assert(result1 == expectedResult1)

    val result2 = dialogueHistory.history().head
    val expectedResult2 = Utterance("Alice", "Hello world !")
    assert(result2 == expectedResult2)
  }

  test("Dialogue history should appropriately score self-repetitions") {
    val history = IndexedSeq(
      Utterance("Alice", "hi Bob ! how are you today ?"),
      Utterance("Bob", "hi Alice ! good and you ?"),
      Utterance("Alice", "good thanks !"),
      Utterance("Alice", "what's up ?"),
      Utterance("Bob", "sorry ?"),
    )
    val dialogueHistory = DialogueHistory(history)
    val newUtterance = Utterance("Alice", "i said what's up ?")

    val result = dialogueHistory.score(newUtterance)

    val expression = Expression(dialign.nlp.Tokenizer.tokenizeWithoutMarkers("what's up ?"))
    val expectedResult = UtteranceScoring(
      utterance = Utterance("Alice", "i said what's up ?"),
      der = 0.0d,
      dser = 3.0d / 5.0d,
      sharedExpressions = Set.empty,
      establishedSharedExpressions = Set.empty,
      selfExpressions = Set(expression))

    assert(result == expectedResult)
  }

  test("Dialogue history should appropriately score verbal alignment") {
    val history = IndexedSeq(
      Utterance("Alice", "hi Bob ! how are you today ?"),
      Utterance("Bob", "hi Alice ! good and you ?"),
      Utterance("Alice", "good thanks !"),
      Utterance("Alice", "what's up ?"),
      Utterance("Bob", "sorry ?"),
      Utterance("Alice", "i said what's up ?"),
      Utterance("Bob", "oh nothing new")
    )
    val dialogueHistory = DialogueHistory(history)
    val newUtterance = Utterance("Bob", "so what's up my dear ?")

    val result = dialogueHistory.score(newUtterance)

    val expression = Expression(dialign.nlp.Tokenizer.tokenizeWithoutMarkers("what's up"))
    val expectedResult = UtteranceScoring(
      utterance = Utterance("Bob", "so what's up my dear ?"),
      der = 2.0d / 6.0d,
      dser = 0.0d,
      sharedExpressions = Set(expression),
      establishedSharedExpressions = Set(expression),
      selfExpressions = Set.empty)

    assert(result == expectedResult)
  }
}
