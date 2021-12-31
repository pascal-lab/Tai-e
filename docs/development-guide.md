## Building Tai-e

Before building Tai-e, make sure that your build tool/IDE uses **JDK 17**.

### Building Tai-e with Gradle
Tai-e uses Gradle as its build system, thus it can be easily built as follows:
1. Install Gradle (we use Gradle 7.3).
2. Type command in Tai-e's root directory: `gradle compileJava`.

### Building Tai-e with IntelliJ IDEA (Recommended)
IntelliJ IDEA supports a fully-functional integration with Gradle, thus it is very easy to [import Tai-e into IntelliJ IDEA](https://www.jetbrains.com/help/idea/gradle.html#gradle_import_project_start).

### Building Tai-e with Eclipse (TODO)

### Building Tai-e with Visual Studio Code (TODO)

---
## How to Develop A New Analysis?
To develop a new analysis and make it available in Tai-e, you need to finish following two steps.

### 1. Develop the analysis
Depending on if the analysis runs on method-, class- or program-level, the analysis class should extends either [MethodAnalysis](../src/main/java/pascal/taie/analysis/MethodAnalysis.java), [ClassAnalysis](../src/main/java/pascal/taie/analysis/ClassAnalysis.java) or [ProgramAnalysis](../src/main/java/pascal/taie/analysis/ProgramAnalysis.java).

In [MethodAnalysis](../src/main/java/pascal/taie/analysis/MethodAnalysis.java), you need to implement the analysis logic in method `analyze(IR)`, which at each time takes the IR of a method as input.

In [ClassAnalysis](../src/main/java/pascal/taie/analysis/ClassAnalysis.java), you need to implement the analysis logic in method `analyze(JClass)`, which at each time takes a class as input.

In [ProgramAnalysis](../src/main/java/pascal/taie/analysis/ProgramAnalysis.java), you need to implement the analysis logic in method `analyze()`. Inter-procedural analyses typically require whole-program information, which can be accessed by the static methods of [World](../src/main/java/pascal/taie/World.java), thus we do not pass argument to the `analyze()` method.

Below we provide some tips which may be useful for developing new analysis.

#### Obtain options
Global options are available at `World.getOptions()`; options with respect to each analysis are dispatched to each `Analysis` object, and can be accessed by `Analysis.getOptions()`.

#### Obtain results of other analyses
If your analysis requires the results of some other previously-executed analyses, you could obtain it by calling `ir.getResult(id)`/`jclass.getResult(id)`/`World.getResult(id)` for method/class/program analysis results.


### 2. Add analysis information to configuration file
To make an analysis available in Tai-e, you need to add its information to [the configuration file](../src/main/resources/tai-e-analyses.yml) ("config file" for short), which contains the information of all available analysis as mentioned in [README](../README.md).

Each analysis in config file consists of five items as described below.

#### `description`: description of the analysis

#### `analysisClass`: fully-qualified name of the analysis class

#### `id`: unique identifier of the analysis

Tai-e relies on analysis id to identify each analysis, so the id of each analysis must be unique. If an id is assigned to multiple analyses, the configuration system will throw `ConfigException`.

#### `requires` (optional): require items of the analysis

Each require item contains two part:

1. analysis id (say `A`), whose result is required by this analysis.
2. require conditions (optional), which are relevant to the options of this analysis. If the conditions are given, then this analysis requires `A` only when all conditions are satisfied.

We support simple compositions of conditions, and we give some examples to illustrate require items:

* `requires: [A1,A2]`: requires analyses `A1` and `A2`
* `requires: [A(x=y)]`: requires `A` when value of option `x` is `y`
* `requires: [A(x=y&a=b)]`: requires `A` when value of option `x` is `y` and value of option `a` is `b`
* `requires: [A(x=a|b|c)]`: requires `A` when value of option `x` is `a`, `b`, or `c`

#### `options`: options of the analysis

During startup stage, Tai-e deserializes configuration of each analysis from config file to an [AnalysisConfig](../src/main/java/pascal/taie/config/AnalysisConfig.java) object, and you could check this class for more details.

After adding analysis information to config file, your analysis is now available in Tai-e.
