# dialign #

Latest version: 0.1 (alpha)

dialign is  a software that  provides automatic and generic  measures of
verbal alignment in  dyadic dialogue based on  sequential pattern mining
at the level of surface of text utterances.

A good place to start can be found in the following paper:
- Dubuisson Duplessis, G.; Clavel, C.; Landragin, F., **Automatic Measures to Characterise Verbal Alignment in Human-Agent Interaction**, 18th Annual Meeting of the Special Interest Group on Discourse and Dialogue (SIGDIAL), 2017, pp. 71--81 \[[See paper](http://www.sigdial.org/workshops/conference18/proceedings/pdf/SIGDIAL10.pdf)\]

dialign  is based  on the  observation  that the  behaviour of  dialogue
participants tend to converge and  automatically align at several levels
(such as the lexical, syntactic  and semantic ones).  One consequence of
successful alignment at several  levels between dialogue participants is
a certain  repetitiveness in  dialogue leading to  the development  of a
lexicon  of   fixed  expressions.   As   a  matter  of   fact,  dialogue
participants tend  to automatically establish and  use fixed expressions
that become dialogue routines.

dialign targets verbal alignment at  the lexical level with a particular
focus on  which words and  lexical patterns are shared  between dialogue
participants. dialign  provides global and speaker-specific  measures of
verbal alignment  based on repetition  at the lexical level  in dialogue
transcripts.

!![Idea of the framework: automatic building of the shared expression lexicon to derive verbal alignment measures](../blob/master/doc/img/framework.png?raw=true)

Essentially,  dialign works  by  automatically building  the lexicon  of
shared  expression from  the transcript  of a  dialogue. The  expression
lexicon keeps  track of shared  expressions and valuable  features about
these  expressions  (e.g.,  who  first  produced  this  expression,  its
frequency).  Then,  straightforward measures  are derived  by leveraging
both the dialogue transcript and the dialogue lexicon.

Global measures are:
- **Expression Lexicon Size  (ELS)**: the number of items  in the expression
  lexicon
- **Expression Variety  (EV)**: the  ELS normalised by  the total  number of
  tokens in the dialogue
- **Expression Repetition (ER)**: the ratio of produced tokens belonging
  to a repetition of an expression

Speaker-specific measures are (for a speaker S):
- **Initiated Expression  (IE_S)**: the  proportion of expressions  of the
  lexicon initiated by S
- **Expression Repetition (ER_S)**: the proportion of tokens produced by
  S that belong to a repetition of an expression

## Installation ##

### From JAR ###

A ready-to-use JAR is available on github. Check the latest release!

### From source code ###

You can generate the JAR from [SBT](http://www.scala-sbt.org/).

First, clone the repository. Then, you can compile the code:

	$ sbt compile

Eventually, you can produce the JAR as follows:

	$ sbt assembly

The JAR file can be probably found in the directory `dialign/target/scala-2.11/`.

## Usage ##
### Input/Output Description ###
dialign takes as input a directory containing dialogue files (1 file per
dialogue).  For each  dialogue  (e.g., "input-transcript.txt"),  dialign
outputs:
- the automatically  built dialogue  lexicon in a  CSV file  suffixed by
  `-lexicon.csv` (e.g., "input-transcript-lexicon.csv")
- a tagged version  of the dialogue transcript with repetitions  in a TXT
  file         suffixed         by        `-dialogue.txt`         (e.g.,
  "input-transcript-dialogue.txt")

Additionally,    dialign     outputs    a    synthesis     file    named
`dial-synthesis.csv` which groups together the computed verbal alignment
measures  for  each  dialogue. 


#### Output: Verbal Alignment Measures ####
The synthesis CSV file `dial-synthesis.csv` includes  the  following
columns:
- `ID`: unique ID of the dialogue file
- `Num. utterances`: number of utterances in the dialogue
- `Num. tokens`: total number of tokens in the dialogue
- `Expression Lexicon Size (ELS)`: the number of items  in the expression lexicon
- `Expression Variety (EV)`:  the ELS normalised by the  total number of
  tokens in the dialogue
- `Expression Repetition  (ER)`: the ratio of  produced tokens belonging
  to a repetition of an expression
- `S1/Initiated Expression (IE_S1)`: the proportion of expressions of the
  lexicon initiated by S1
- `S1/Expression Repetition (ER_S1)`: the  proportion of tokens produced
  by S1 that belong to a repetition of an expression
- `S1/tokens (%)`: the proportion of tokens  produced by S1 in the whole
  dialogue
- `S2/Initiated Expression  (IE_S2)`: the  proportion of  expressions of
  the lexicon initiated by S2
- `S2/Expression Repetition (ER_S2)`: the  proportion of tokens produced
  by S2 that belong to a repetition of an expression
- `S2/tokens (%)`: the proportion of tokens  produced by S2 in the whole
  dialogue
- `Voc. Overlap`: vocabulary overlap
- `Voc. Overlap S1`: relative shared vocabulary by S1
- `Voc. Overlap S2`: relative shared vocabulary by S2

Note that  dialign refers to  the speakers as `S1`  and `S2`. S1  is the
speaker  that comes  first in  the alpha-numerical  ordering, S2  is the
speaker that comes next. For instance,  if a dialogue involves "Bob" and
"Alice", "Alice" is referred as S1 and "Bob" as S2.

#### Output: Dialogue Lexicon ####
For each dialogue, dialign outputs its expression lexicon. This CSV file contains the following columns:
- `Freq.`: the number of different utterances in which the expression appears	
- `Size`: the number of tokens of the expression
- `Surface Form`: the surface form of the expression
- `Establishment turn`: the turn number in which the expression is first repeated (counting starts from 0)
- `Spanning`: the number of utterances between the first production and the last production of the expression
- `Priming`: the number of repetitions of the expression by the initiator before being used by the other interlocutor
- `First Speaker`: the initiator of the expression
- `Turns`: the turns in which the expression appears

### Before Using dialign on Your Data: Dialogue Formatting ###

Before using dialign on your data, make sure that dialogues are correctly formatted to be processed by dialign.
- Each dialogue file should contain one and only one dialogue.
- When computing verbal alignment measures  for an entire corpus, make
  sure that the  locutor names are normalized  between dialogues (for
  instance, that  the system is  always referred  to as "S"  and the
  user as "U").

Importantly, each utterance should be already:
- tokenized (a space must separate tokens)
- normalized (each token must be normalized, e.g. regarding lower/uppercase)

Each line in the dialogue file should comply to the following format:
```text
LOCUTOR:\tTOKENIZED UTTERANCE\n
```
where:
- `LOCUTOR`: the unique identifier of the locutor
- `TOKENIZED UTTERANCE`: the (already) tokenized and normalized utterance 

E.g., it should look like:
```text
Alice:	hello ! how are you today ?
Bob:	hello Alice ! I'm fine , how are you ?
Alice:	I'm fine , thank you !
```

### Usage Examples ###

Let's  say  that   the  dialogue  files  are  in   the  input  directory
`input-directory/`  and   that  output  is  planned   in  the  directory
`output-directory/`. To run dialign  with this configuration, proceed as
follows:
```bash
java -jar dialign.jar -i input-directory/ -o output-directory/
```
The synthesis file is located at: `output-directory/dial-synthesis.csv`


dialign  allows to  filter input  dialogue files  by prefix,  suffix and
extension. For  instance, if the  only input dialogue files  to consider
are files matching the  following pattern: `dialogue-*-cleaned.dial`, it
is possible use the following options with dialign:
```bash
java -jar dialign.jar -i input-directory/ -o output-directory/ \
	-p "dialogue-" \ # specification of a required filename prefix
	-s "-cleaned" \ # specification of a required filename suffix
	-e "dial" # specification of the extension (without the '.')
```

More options are available, see usage note:
```bash
java -jar dialign.jar -h
```

## Contributors ##

- Guillaume Dubuisson Duplessis (2017)

## Usage for Research Purposes ##

If you use this software for  research purposes, please make reference to
it by citing the following paper:
- Dubuisson Duplessis, G.; Clavel, C.; Landragin, F., **Automatic Measures to Characterise Verbal Alignment in Human-Agent Interaction**, 18th Annual Meeting of the Special Interest Group on Discourse and Dialogue (SIGDIAL), 2017, pp. 71--81 \[[See paper](http://www.sigdial.org/workshops/conference18/proceedings/pdf/SIGDIAL10.pdf)\]

The authors  of this work would  be happy to  hear about you if  you are
using this code! Please, do not hesitate to contact us:
- G. Dubuisson Duplessis <[website](http://www.dubuissonduplessis.fr/contact.html)>
- C. Clavel <[website](https://clavel.wp.imt.fr/)>
- F. Landragin <[website](http://fred.landragin.free.fr/)>

## License ##

CECILL-B - see the LICENSE file.
