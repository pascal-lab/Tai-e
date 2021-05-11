## Building Tai-e

Before building Tai-e, make sure that your build tool/IDE uses **JDK 11** (*TODO: check higher JDK versions*).

### Building Tai-e with Gradle
Tai-e uses Gradle as its build system, thus it can be easily built as follows:
1. Install Gradle (we use Gradle 5.6.2, *TODO: check other Gradle versions*).
2. Switch working directory to Tai-e's root directory.
3. Run command: `gradle compileJava`.

### Building Tai-e with IntelliJ IDEA (Recommended)
IntelliJ IDEA supports a fully-functional integration with Gradle, thus it is very easy to [import Tai-e into IntelliJ IDEA](https://www.jetbrains.com/help/idea/gradle.html#gradle_import_project_start).

### Building Tai-e with Eclipse (TODO)

### Building Tai-e with Visual Studio Code (TODO)



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
