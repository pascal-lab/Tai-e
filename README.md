# Tai-e: An Easy-to-Learn/Use Static Analysis Framework for Java

## Description
Tai-e is an easy-to-learn, easy-to-use, efficient and extensible static analysis framework for Java.

---
## How to Run Tai-e?

### Prerequisites
Before running Tai-e, please finish following steps:

- Install **Java 11** on your system (Tai-e is developed in pure Java, and it runs on all major operating systems including Windows/Linux/MacOS).
- Clone submodule `java-benchmarks` (it contains the Java libraries used by the analysis):
```
  git submodule update --init --recursive
```

---
The main class (entry) of Tai-e is `pascal.taie.Main`. Below we introduce its main options.

### Program Options
These options specify the program (say *P*) and Java library to be analyzed.

#### Class paths (-cp, --class-path): `-cp <path>;<path>;...`
Class paths for Tai-e to locate the classes of *P*. Currently, Tai-e supports following types of paths:

- relative/absolute path to a jar file
- relative/absolute path to a directory which contains `.class` files

Multiple paths are separated by path separator. Note that the path separator varies on different systems: it is `;` on Windows, and `:` on Unix-like systems.

#### Main class (-m, --main-class): `-m <main-class>`
The main class (entry) of *P*. This class must declare a method with signature `public static void main(String[])`.

#### Java version (-java): `-java <version>`
Specify the version of Java library used in the analyses. When this option is given, Tai-e will locate the corresponding Java library in submodule `java-benchmarks` and add it to the class paths. Currently, we provide libraries for Java version 3, 4, 5, 6, 7, 8.

#### Prepend JVM Class Path (-pp, --prepend-JVM)
Prepend the class path of the JVM (which runs Tai-e) to the analysis class path. This option will disable `-java` option, i.e., when `-pp` is enabled, Tai-e always analyzes the Java library of the JVM, no matter what `-java` is.

### Analysis Options
These options specify the analyses to be executed by Tai-e. To execute an analysis, you need to specify its *id* and *options* (if necessary). All available analyses in Tai-e and their information (e.g., *id* and available *options*) are listed in [the analysis configuration file](src/main/resources/tai-e-analyses.yml).

There are two mutually-exclusive approaches to specify the analyses, by options or by file, as described below.

#### Analyses (-a, --analysis): `-a <id>[=<key>:<value>,...]`
Specify analyses by options. If you need to run an analysis `A`, just use `-a A`. If you need to specify some analysis options for `A`, just append them to analysis id (connected by `=`), and separate them by `,`, for example:
```
-a A=enableX:true,threshold:100,log-level:info
```
The option system is flexible, and it supports various types of option values, such as boolean, integer, and string.

Option `-a` is repeatable, so that if you need to execute multiple analyses in one run of Tai-e, for example, analysis `A2` requires the result of analysis `A1`, just repeat `-a` like: `-a A1 -a A2`.

#### Plan file (-p, --plan-file): `-p <file-path>`
Specify analyses by file. You can specify the analyses to be executed (called an analysis plan) in a plan file, and use `-p` to process the file. Similar to `-a`, you need to specify the *id* and *options* (if necessary) for each analysis in the file. The plan file should be written in YAML.

Note that options `-a` and `-p` are mutually-exclusive, thus you *cannot* specify them simultaneously.

(*TODO: elaborate analysis system in more detailed level*)

### Other Options
#### Help (-h, --help)
Print help information for all available options. This option will disable all other options, i.e., when it is enabled, Tai-e ignores other given options.

#### Options file (--options-file): `--options-file <file-path>`
You can specify the options in a options file and use `--options-file` to process the file. When this option is given, Tai-e ignores all other command-line options, and only processes the options in the file. The options file should be written in YAML.

### Putting It Together
We give an example of how to analyze a program by Tai-e. Suppose we want to analyze a program *P* as described below:

- *P* consists of two jar files: `foo.jar` and `bar.jar`
- *P*'s main class is `baz.Main`
- *P* is analyzed together with Java 8
- we run 2-object-sensitive pointer analysis and merge all string constants in *P*
- we run Tai-e on Windows

Then the options would be:
```
java pascal.taie.Main -cp foo.jar;bar.jar -m baz.Main -java 8 -a pta=cs:2-obj,merge-string-constants:true
```
(for simplicity, we omit the Java options for Tai-e)

---
## How to Develop New Analyses Based on Tai-e?

Please refer to [development guide](docs/development-guide.md).
