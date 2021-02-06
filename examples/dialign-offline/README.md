# Tutorial to use `dialign` for Corpus Studies

In this tutorial, you will:
- learn [how to format your dialogue transcripts in `tsv` format](#input-data-dialogue-transcripts) ;
- learn [how to run `dialign` on a single dialogue transcript](#running-dialign) (generalisation to a 
  full corpus is straightforward) ; and
- understand the output files of `dialign` where you can find 
   + [the measures provided by `dialign` for the full corpus in `tsv` format](#output-files-at-the-level-of-the-corpus),
   + [the shared expression lexicon in `tsv` format for each dialogue transcript](#the-shared-expression-lexicon), and
   + [the self-repetition lexicons in `tsv` format for each dialogue transcript](#the-self-repetition-lexicons).

## Input Data: Dialogue Transcripts

In this tutorial, the input corpus is located in the [corpus/ directory](dialign-offline/corpus/). It contains
a single transcript "[excerpt-alice.tsv](corpus/excerpt-alice.tsv)" which content is reproduced
below:
```tsv
Human:	okay well but the books like  there are many eh facts in the book which are against the law of physics like the piece of cake which is which makes eh Alice bigger or smaller i don't remember but which
Agent:	indeed
Human:	yeah eh does the piece of cake makes Alice bigger or smaller ?
Agent:	the piece of cake grows Alice
Human:	grows Alice okay and what makes her smaller ?
Agent:	sorry could you repeat that ?
Human:	what makes eh Alice smaller ?
Agent:	throughout the book there are many making her smaller
Human:	i'm sorry i didn't understand
Agent:	throughout the book there are many passages where Alice shrinks
```

### Input Format
The dialogue format used by `dialign` is the 
[`tsv` format (tab-separated values)](https://en.wikipedia.org/wiki/Tab-separated_values)
with the first column being the speaker and the second column being the utterance.
In other words, each line in the dialogue file should comply to the following format:
```text
LOCUTOR:\tTOKENIZED UTTERANCE\n
```
where:
- `LOCUTOR`: the unique identifier of the locutor
- `TOKENIZED UTTERANCE`: the (already) tokenized and normalized utterance 

### Preprocessing of the Transcripts
Before using `dialign` on your data, make sure that the dialogues are correctly formatted to be 
processed by `dialign`.
- Each dialogue file should contain one and only one dialogue.
- When computing verbal alignment measures  for an entire corpus, make
  sure that the  locutor names are normalized  between dialogues (for
  instance, that  the system is  always referred  to as "S"  and the
  user as "U").

Importantly, each utterance should be already:
- tokenized (a space must separate tokens)
- normalized (each token must be normalized, e.g. regarding lower/uppercase)

## Running dialign

In this tutorial, the input corpus is the `corpus/` directory. Let's say we want `dialign`
to output the results in the `output-dialign/` directory. To this end, simply run `dialign`
as follows:
```bash
java -jar dialign.jar -i corpus/ -o output-dialign/
```

Note: to run dialign on a corpus containing multiple dialogue transcripts, you simply need 
to put the dialogue transcripts in the input directory (here `corpus/`).

## Output Data: Description of dialign Output Files

`dialign` outputs several files as can be seen in the 
[output-dialign/ directory](output-dialign/). Output files can be divided into
two parts:
1. [measures for the entire corpus](#output-files-at-the-level-of-the-corpus), and
1. [the lexicons for each dialogue transcript](#output-files-for-each-dialogue-transcript).

### Output Files at the Level of the Corpus
Output files at the level of the corpus can ben broken down into:
- speaker-independent measures, and
- speaker dependant measures.

Please refer to the section ["measures provided by `dialign`"](../../README.md#measures-provided-by-dialign)
for a list of the provided measures.

#### Speaker-independent Measures
Speaker-independent measures --which are related to the interactive verbal alignment 
process-- are available in the file named 
[metrics-speaker-independent.tsv](output-dialign/metrics-speaker-independent.tsv)
which content is reproduced below:
```tsv
ID	Num. utterances	Num. tokens	Expression Lexicon Size (ELS)	Expression Variety (EV)	Expression Repetition (ER)	Voc. Overlap	ENTR	L	LMAX
excerpt-alice_tsv	10.0	105.0	11.0	0.10476190476190476	0.2	0.28	1.7566238110793635	1.88	4.0
```

This file includes  the  following columns:
- `ID`: unique ID of the dialogue file
- `Num. utterances`: number of utterances in the dialogue
- `Num. tokens`: total number of tokens in the dialogue
- `Expression Lexicon Size (ELS)`: the number of items  in the shared expression lexicon
- `Expression Variety (EV)`:  the ELS normalised by the  total number of
  tokens in the dialogue
- `Expression Repetition  (ER)`: the ratio of  produced tokens belonging
  to a repetition of an expression
- `Voc. Overlap`: vocabulary overlap
- `ENTR`: the complexity of the shared expression lexicon
- `L`: average length in token of the shared expression instances
- `LMAX`: maximum length in token of the shared expression instances


#### Speaker-dependent Measures
Speaker-dependent measures --which are related to both the interactive verbal alignment 
process and the self-repetition behaviour-- are available in the file named 
[metrics-speaker-dependent.tsv](output-dialign/metrics-speaker-dependent.tsv)
which content is reproduced below:
```tsv
ID	S1	S2	S1/Initiated Expression (IE_S1)	S1/Expression Repetition (ER_S1)	S1/tokens (%)	S2/Initiated Expression (IE_S2)	S2/Expression Repetition (ER_S2)	S2/tokens (%)	Voc. Overlap S1	Voc. Overlap S2	SR/S1/ELS	SR/S1/EV	SR/S1/ER	SR/S1/ENTR	SR/S1/L	SR/S1/LMAX	SR/S2/ELS	SR/S2/EV	SR/S2/ER	SR/S2/ENTR	SR/S2/L	SR/S2/LMAX
excerpt-alice_tsv	Agent	Human	0.18181818181818182	0.53125	0.3047619047619048	0.8181818181818182	0.0547945205479452	0.6952380952380952	0.5833333333333334	0.35	3.0	0.08108108108108109	0.40540540540540543	0.9709505944546686	3.0	6.0	12.0	0.15384615384615385	0.5256410256410257	1.7814164026211206	2.0	4.0
```

This file includes  the  following columns:
- `ID`: unique ID of the dialogue file
- `S1`: speaker referred to as `S1`
- `S2`: speaker referred to as `S2`
- Measures related to the interactive verbal alignment process
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
- Measures related to the self-repetition behaviour
   - `SR/S1/ELS`: the number of items in the self-repetition lexicon for `S1`
   - `SR/S1/EV`: Self-Expression Variety (SEV) for `S1`
   - `SR/S1/ER`: Self-Expression Repetition (SER) for `S1`
   - `SR/S1/ENTR`: complexity of the self-repetitions for `S1`
   - `SR/S1/L`: average length in tokens of the self-repetitions for `S1`
   - `SR/S1/LMAX`: maximum length in token of the self-repetitions for `S1`
   - `SR/S2/ELS`: the number of items in the self-repetition lexicon for `S2`
   - `SR/S2/EV`: Self-Expression Variety (SEV) for `S2`
   - `SR/S2/ER`: Self-Expression Repetition (SER) for `S2`
   - `SR/S2/ENTR`: complexity of the self-repetitions for `S2`
   - `SR/S2/L`: average length in tokens of the self-repetitions for `S2`
   - `SR/S2/LMAX`: maximum length in token of the self-repetitions for `S2`

### Output Files for each Dialogue Transcript

For each dialogue transcript in the corpus (here: `excerpt-alice.tsv`), `dialign` outputs:
- a tagged version of the dialogue transcript, here: 
[excerpt-alice_tsv-dialogue.txt](output-dialign/excerpt-alice_tsv-dialogue.txt);
- the shared expression lexicon, here: 
[excerpt-alice_tsv-lexicon.tsv](output-dialign/excerpt-alice_tsv-lexicon.tsv);
- for each speaker, the self-repetition lexicon, here:
   + for S1: [excerpt-alice_tsv-lexicon-self-rep-A.tsv](output-dialign/excerpt-alice_tsv-lexicon-self-rep-A.tsv) ; and
   + for S2: [excerpt-alice_tsv-lexicon-self-rep-B.tsv](output-dialign/excerpt-alice_tsv-lexicon-self-rep-B.tsv).

#### The Tagged Version of the Dialogue Transcript
The tagged version of the dialogue transcript, here
[excerpt-alice_tsv-dialogue.txt](output-dialign/excerpt-alice_tsv-dialogue.txt), provides
a detailed view of the occurrence of shared expressions in the dialogue transcripts.

An excerpt of the content of this file is reproduced below:
```text
B|0| Human: okay well but _the_ books like _there are many_ eh facts in _the book_ which _are_ against _the_ law _of_ physics like _the piece of cake_ which is which makes eh _Alice_ bigger or _smaller_ i don't remember but which 
A|1| Agent: indeed 
B|2| Human: yeah eh does _the piece of cake_ makes _Alice_ bigger or _smaller_ ? 
A|3| Agent: [the piece of cake] _grows Alice_ 
B|4| Human: [grows Alice] okay and what makes _her smaller_ ? 
A|5| Agent: _sorry_ could you repeat that ? 
B|6| Human: what makes eh [Alice] _smaller_ ? 
A|7| Agent: throughout [the book] [there are many] making [her smaller] 
B|8| Human: i'm [sorry] i didn't understand 
A|9| Agent: throughout [the book] [there are many] passages where [Alice] shrinks 
```

First occurrence of a shared expression is marked by underline characters
 "\_...\_". 
Repetition of a shared expression is marked by brackets "[...]".

#### The Shared Expression Lexicon
The shared expression lexicon provides a detailed view of the shared expressions 
in `tsv` format.
The content of [excerpt-alice_tsv-lexicon.tsv](output-dialign/excerpt-alice_tsv-lexicon.tsv), 
is reproduced below:
```tsv
Freq.	Free Freq.	Size	Surface Form	Establishment turn	Spanning	Priming	First Speaker	Turns
3	3	4	the piece of cake	3	4	2	Human	0, 2, 3
3	3	3	there are many	7	10	1	Human	0, 7, 9
3	3	2	the book	7	10	1	Human	0, 7, 9
2	2	2	grows Alice	4	2	1	Agent	3, 4
2	2	2	her smaller	7	4	1	Human	4, 7
6	4	1	Alice	3	10	2	Human	0, 2, 3, 4, 6, 9
5	3	1	smaller	7	8	4	Human	0, 2, 4, 6, 7
5	1	1	the	3	10	2	Human	0, 2, 3, 7, 9
3	1	1	are	7	10	1	Human	0, 7, 9
3	1	1	of	3	4	2	Human	0, 2, 3
2	2	1	sorry	8	4	1	Agent	5, 8
```

This `tsv` file contains the following columns:
- `Freq.`: the number of different utterances in which the shared expression appears
- `Free Freq.`: the number of different utterances in which the shared expression appears in a free form	
- `Size`: the number of tokens of the expression
- `Surface Form`: the surface form of the expression
- `Establishment turn`: the turn number in which the expression is first repeated (first turn is 0)
- `Spanning`: the number of utterances between the first production and the last production of the expression
- `Priming`: the number of repetitions of the expression by the initiator before being used by the other interlocutor
- `First Speaker`: the initiator of the expression
- `Turns`: the turns in which the expression appears

#### The Self-Repetition Lexicons
The self-repetition lexicons provide a detailed view of the self-repetitions for each
dialogue participant in `tsv` format. Here, they are available:
+ for S1 in [excerpt-alice_tsv-lexicon-self-rep-A.tsv](output-dialign/excerpt-alice_tsv-lexicon-self-rep-A.tsv) ; and
+ for S2 in [excerpt-alice_tsv-lexicon-self-rep-B.tsv](output-dialign/excerpt-alice_tsv-lexicon-self-rep-B.tsv).

The column definitions are the same as the ones for 
[the shared expression lexicon](examples#the-shared-expression-lexicon).

The content of `excerpt-alice_tsv-lexicon-self-rep-A.tsv` is reproduced below:
```tsv
Freq.	Size	Surface Form	Spanning	First Speaker	Turns
2	6	throughout the book there are many	3	Agent	7, 9
3	1	the	7	Agent	3, 7, 9
2	1	Alice	7	Agent	3, 9
```

The content of `excerpt-alice_tsv-lexicon-self-rep-B.tsv` is reproduced below:
```tsv
Freq.	Size	Surface Form	Spanning	First Speaker	Turns
2	4	Alice bigger or smaller	3	Human	0, 2
2	4	the piece of cake	3	Human	0, 2
2	3	makes eh Alice	7	Human	0, 6
3	2	smaller ?	5	Human	2, 4, 6
2	2	what makes	3	Human	4, 6
4	1	Alice	7	Human	0, 2, 4, 6
4	1	makes	7	Human	0, 2, 4, 6
3	1	eh	7	Human	0, 2, 6
2	1	i	9	Human	0, 8
2	1	of	3	Human	0, 2
2	1	okay	5	Human	0, 4
2	1	the	3	Human	0, 2
```

## That's all folks!
You know everything you need to know to use `dialign` for corpus studies! :-)
