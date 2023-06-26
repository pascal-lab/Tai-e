<div align="center">
  <img src="tai-e-logo.png" height="200">

## Tai-e

</div>

## What is Tai-e?

Tai-e (Chinese: 太阿; pronunciation: [ˈtaɪə:]) is a new static analysis framework for Java (please
see our [ISSTA 2023 paper](https://cs.nju.edu.cn/tiantan/papers/issta2023.pdf) for details), which features arguably
the "best" designs from both the novel ones we proposed and those of classic frameworks such as
Soot, WALA, Doop, and SpotBugs. Tai-e is easy-to-learn, easy-to-use, efficient, and highly
extensible, allowing you to easily develop new analyses on top of it.

Currently, Tai-e provides the following major analysis components (and more analyses are on the
way):

- Powerful pointer analysis framework
    - On-the-fly call graph construction
    - Various classic and advanced techniques of heap abstraction and context sensitivity for
      pointer analysis
    - Extensible analysis plugin system (allows to conveniently develop and add new analyses that
      interact with pointer analysis)
- Various fundamental/client/utility analyses
    - Fundamental analyses, e.g., reflection analysis and exception analysis
    - Modern language feature analyses, e.g., lambda and method reference analysis, and
      invokedynamic analysis
    - Clients, e.g., configurable taint analysis (allowing to configure sources, sinks and taint
      transfers)
    - Utility tools like analysis timer, constraint checker (for debugging), and various graph
      dumpers
- Control/Data-flow analysis framework
    - Control-flow graph construction
    - Classic data-flow analyses, e.g., live variable analysis, constant propagation
    - Your data-flow analyses
- SpotBugs-like bug detection system
    - Bug detectors, e.g., null pointer detector, incorrect `clone()` detector
    - Your bug detectors

Tai-e is developed in Java, and it can run on major operating systems including Windows, Linux, and
macOS.

As a courtesy to the developers, we expect that you **please [cite](docs/bibtex.txt) the paper** from ISSTA 2023 describing the Tai-e framework in your research work:

Tian Tan and Yue Li. 2023.
**Tai-e: A Developer-Friendly Static Analysis Framework for Java by Harnessing the Good Designs of Classics.**
In Proceedings of the 32nd ACM SIGSOFT International Symposium on Software Testing and Analysis (ISSTA '23), July 17–21, 2023, Seattle, WA, United States ([pdf](https://cs.nju.edu.cn/tiantan/papers/issta2023.pdf), [bibtex](docs/bibtex.txt)).

## How to Obtain Runnable Jar of Tai-e?

The simplest way is to download it
from [GitHub Releases](https://github.com/pascal-lab/Tai-e/releases).

Alternatively, you might build the latest Tai-e yourself from the source code. This can be simply
done via Gradle (be sure that Java 17 (or higher version) is available on your system). You just
need to run command `gradlew fatJar`, and then the runnable jar will be generated in `tai-e/build/`,
which includes Tai-e and all its dependencies.

## Documentation

### Wiki
We are hosting the documentation of Tai-e
on [the GitHub wiki](https://github.com/pascal-lab/Tai-e/wiki), where you could find more
information about Tai-e such
as [Setup in IntelliJ IDEA](https://github.com/pascal-lab/Tai-e/wiki/Setup-Tai%E2%80%90e-in-IntelliJ-IDEA)
, [Command-Line Options](https://github.com/pascal-lab/Tai-e/wiki/How-to-Run-Tai%E2%80%90e%3F-(command%E2%80%90line-options))
,
and [Development of New Analysis](https://github.com/pascal-lab/Tai-e/wiki/How-to-Develop-A-New-Analysis-on-Tai%E2%80%90e%3F)
.

### Changelog
Since we are actively developing and updating Tai-e, we record the notable changes we made, especially the new features and breaking changes, in [CHANGELOG](CHANGELOG.md). If you find something wrong after updating Tai-e, maybe you could check [CHANGELOG](CHANGELOG.md) for useful information.

## Tai-e Assignments

In addition, we have developed
an [educational version of Tai-e](http://tai-e.pascal-lab.net/en/intro/overview.html) where eight
programming assignments are carefully designed for systematically training learners to implement
various static analysis techniques to analyze real Java programs. The educational version shares a
large amount of code with Tai-e, thus doing the assignments would be a good way to get familiar with
Tai-e.
