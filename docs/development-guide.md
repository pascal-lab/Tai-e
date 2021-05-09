### Prerequisites
Before you start develop your analysis on top of Tai-e, please finish following steps:
- Install Java 11 on your system (Windows/Linux/MacOS)
- Import to IntelliJ IDEA as a Gradle project
- Clone submodule `java-benchmarks` (it contains the Java libraries used by the analysis) by
```
git submodule update --init --recursive
```

### How to Develop A New Analysis?

**1. Develop the analysis**

`pascal.taie.analysis.IntraproceduralAnalysis`
`pascal.taie.analysis.InterproceduralAnalysis`

**2. Add analysis information to configuration file.**
