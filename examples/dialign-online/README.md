# Tutorial to try `dialign-online` for Interactive Purposes

In this tutorial, you will:
- learn [how to run `dialign-online` in interactive mode and export the transcript of the created dialogue](#running-dialign-online-in-interactive-mode) ; and
- learn [how to run `dialign-online` on a single dialogue transcript](#running-dialign-online-on-a-dialogue-transcript) 
  in order to directly compute online metrics for each turn.
  
## Running `dialign-online` in Interactive Mode
`dialign-online` can be ran in interactive mode to simulate an ongoing chat dialogue.

### Input Transcript (optional)
`dialign-online` can take an optional dialogue transcript that represents the
starting point of the dialogue. If no dialogue transcript is provided, `dialign-online`
starts from an empty dialogue.

In this tutorial, the transcript "[excerpt-alice-partial.tsv](excerpt-alice-partial.tsv)" 
is used as a starting point. Its content is reproduced below:
```tsv
Human:	okay well but the books like  there are many eh facts in the book which are against the law of physics like the piece of cake which is which makes eh Alice bigger or smaller i don't remember but which
Agent:	indeed
Human:	yeah eh does the piece of cake makes Alice bigger or smaller ?
Agent:	the piece of cake grows Alice
Human:	grows Alice okay and what makes her smaller ?
Agent:	sorry could you repeat that ?
Human:	what makes eh Alice smaller ?
```

The dialogue format used by `dialign-online` is the same
[`tsv` format (tab-separated values)](https://en.wikipedia.org/wiki/Tab-separated_values)
as the [one used for corpus studies](../dialign-offline/README.md#input-format).

### Running an Interactive Session
To start an interactive session, just run `dialign-online` with or without a dialogue
transcript:
```bash
java -jar dialign-online.jar -f excerpt-alice-partial.tsv
# if no dialogue transcript is provided, simply do:
# java -jar dialign-online.jar
```

Results :

![Screenshot of `dialign-online` after loading a dialogue transcript](screenshots/01.png)

`dialign-online` prints the current dialogue history and then prompts for the next
step. To obtain a description of the possible options, just enter "h" (as "help"):

![Screenshot of `dialign-online` after entering "h"](screenshots/02.png)

### Scoring and Adding Utterances

Next, let's try to score a new utterance with the following features:
- Locutor: "Agent"
- Utterance: "throughout the book there are many making her smaller"

To do that, enter "s" (as "score") and specify the "locutor" and the "utterance".
This returns the following result:

![Screenshot of `dialign-online` after scoring an utterance](screenshots/03-2.png)

Scoring the utterance returns multiple information:
- information related to verbal alignement:
   + DER (Dynamic Expression Repetition) which scores between 0.0 (not all) and 
     1.0 (completely) how much the utterance reuses shared lexical patterns
   + Reused shared lexical patterns: the list of shared lexical patterns that
     are reused in this utterance
   + Established shared lexical patterns: the list of shared lexical patterns that
     are reused for the first time (thus "established")
- information related to self-repetitions:
   + DSER (Dynamic Self-repetition Expression Repetition):  which scores between 0.0 (not all) and 
     1.0 (completely) how much the utterance reuses self-repetitions
   + Reused self-repetition patterns: the list of self-repetitions that are reused in
     this utterance.
     
Then, the scored utterance can be added to the dialogue history ("y") or 
discarded ("n", default choice). Here, let's add the utterance ("y"):
![Screenshot of `dialign-online` after adding a scored utterance](screenshots/04.png)

It is also possible to directly add an utterance by pressing "a". For instance,
let's directly add the following utterance:
- Locutor: "Human"
- Utterance: "i'm sorry i didn't understand"

![Screenshot of `dialign-online` for directly adding an utterance](screenshots/05.png)

Directly adding this utterance gives:

![Screenshot of `dialign-online` after directly adding an utterance](screenshots/06.png)

Then, let's score the following agent response:
- Locutor: "Agent"
- Utterance: "throughout the book there are many passages where Alice shrinks"

![Screenshot of `dialign-online` after directly adding an utterance](screenshots/07-1.png)

This gives the following scoring results:

![Screenshot of `dialign-online` after directly adding an utterance](screenshots/07-2.png)

Note that this last utterance both verbally aligns and verbally self-repeats.
Let's add the utterance to the dialogue history by pressing "y".

### Exporting the Created Dialogue Transcript and Closing the Interactive Session

To conclude this tutorial, let's **export the created dialogue**. To do this, simply
press "e" and enter a filepath:

![Screenshot of `dialign-online` after directly adding an utterance](screenshots/08-2.png)

Exporting produces a `tsv` file with the following columns (the full file is available at 
[`dialign-online-excerpt-alice.tsv`](dialign-online-excerpt-alice.tsv)):
```tsv
locutor	utterance	der	sharedExpressions	establishedSharedExpressions	dser	selfRepetitions
```
- `locutor`: the locutor of the utterance
- `utterance`: the text content of the utterance
- `der`: the "Dynamic Expression Repetition" score of the utterance
- `sharedExpressions`: the list of shared lexical patterns reused in this utterance
- `establishedSharedExpressions`: the list of shared lexical patterns established in 
                                  this utterance
- `dser`: the "Dynamic Self-repetition Expression Repetition" score of the utterance
- `selfRepetitions`: the list of lexical self-repetitions used in the utterance

Finally, the interactive session can be closed by pressing "q" (as "quit").

## Running `dialign-online` on a Dialogue Transcript
`dialign-online` can be ran directly on a file to compute online metrics for each 
turn.

To do this, an input dialogue transcript and an output `tsv` file should be provided.
For instance with:
- input dialogue transcript: `excerpt-alice-partial.tsv`
- output `tsv` file: `dialign-online-excerpt-alice-partial.tsv`

`dialign-online` can be executed as follows:
```bash
java -jar dialign-online.jar -f excerpt-alice-partial.tsv -o dialign-online-excerpt-alice-partial.tsv
```

The output file format is the same as exporting from the interactive session 
([see above](#exporting-the-created-dialogue-transcript-and-closing-the-interactive-session)).