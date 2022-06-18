# CHANGELOG

## [1.1] - 2022-06-18

This is a technical maintenance release of the `dialign` software.

Changes:
- Making `dialign` compatible with Windows (GuillaumeDD/dialign#2)
- Upgrade of the main dependencies

## [1.0] - 2021-02-07

This is a major update of the `dialign` software due to the update of the framework
presented in the upcoming LRE journal article.

### Update of the Framework
The framework has been significantly improved. `dialign` now provides a set of 
measures to characterise both:
- the **interactive verbal alignment** process between dialogue participants, and
- the **self-repetition behaviour** of each participant.

These measures allow the characterisation of the nature of these processes by 
addressing various informative aspects such as their variety, strength, complexity, 
stability, and orientation. In a nutshell:
- **variety**: the variety of shared expressions or self-repetitions emerging during a 
  dialogue relative to its length. It is directly related to the number of unique 
  expressions in a lexicon.
- **strength**: the strength of repetition of the (shared) lexical patterns, i.e., how 
  much the patterns are reused.
- **complexity**: the complexity indicates the variety of the types of lexical patterns. 
  It is here featured by Shannon entropy measures. High entropy indicates the presence 
  of a wide range of lexical patterns relative to their lengths in number of tokens 
  (e.g., ranging from a single word to a full sentence). On the contrary, low entropy 
  indicates the predominance of one type of lexical pattern.
- **extension** and **stability**: The extension and stability of the (shared) lexical patterns
  are related to the size of the lexical patterns. The extension indicates the size of 
  the lexical patterns. The longer it is, the more extended the lexical pattern is. 
  Extension is directly linked to the stability of the processes since the more 
  extended the patterns are, the more stable the processes are.
- **orientation**: the orientation of the interactive alignment process, i.e., it indicates 
  either a symmetry (both dialogue participants initiate and reuse the same number of 
  shared lexical patterns), or an asymmetry (a dialogue participant initiates and/or 
  reuses more shared lexical patterns).

### New Application to Demonstrate Online Usage
A new `dialign-online` application is available to demonstrate the capabilities 
of the framework in an online usage in an interactive system.

### Rewriting of Documentation
The documentation has been rewritten. It features:
- [A summary of the framework](README.md#framework)
- A set of tutorials to use `dialign`:
   + [A tutorial to use `dialign` for corpus studies](README.md#dialign-for-corpus-studies)
   + [A demonstration of `dialign-online` for interactive purposes](README.md#dialign-online-for-interactive-purposes)

### Misc
- Upgrade of the dependencies
- Porting from scala 2.11 to scala 2.13

## [0.1, 0.1.1] - 2017-08-29
* Public release of the alpha version of the software

