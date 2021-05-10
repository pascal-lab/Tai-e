### Prerequisites
Before you start develop your analysis on top of Tai-e, please finish following steps:
- Install Java 11 on your system (Windows/Linux/MacOS)
- Import to IntelliJ IDEA as a Gradle project
- Clone submodule `java-benchmarks` (it contains the Java libraries used by the analysis):
```
git submodule update --init --recursive
```

### How to Develop A New Analysis?

**1. Develop the analysis**


`pascal.taie.analysis.IntraproceduralAnalysis`
`pascal.taie.analysis.InterproceduralAnalysis`

`Options` is available at `World.getOptions()`, `AnalysisOptions` will be dispatched to each `Analysis` object.

**2. Add analysis information to configuration file.**

`pascal.taie.config.AnalysisConfig`
- description: 
- analysisClass: fully-qualified name of the analysis class
- id: unique identifier of the analysis
- requires (optional):
- options: default options of the analysis
