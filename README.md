<div align="center">
  <img src="tai-e-logo.png" height="200">

# Tai-e

[![test](https://github.com/pascal-lab/Tai-e/actions/workflows/test.yml/badge.svg)](https://github.com/pascal-lab/Tai-e/actions/workflows/test.yml)
[![java](https://img.shields.io/badge/Java-17-informational)](https://openjdk.java.net/)
[![maven-central](https://img.shields.io/badge/dynamic/xml.svg?label=maven-central&color=f1834d&query=//metadata/versioning/latest&url=https://repo1.maven.org/maven2/net/pascal-lab/tai-e/maven-metadata.xml)](https://search.maven.org/artifact/net.pascal-lab/tai-e)
[![codecov](https://codecov.io/gh/pascal-lab/Tai-e/branch/master/graph/badge.svg)](https://codecov.io/gh/pascal-lab/Tai-e)
[![DOI](https://img.shields.io/badge/DOI-10.1145/3597926.3598120-blue)](https://doi.org/10.1145/3597926.3598120)
</div>

## Table of Contents

- [What is Tai-e?](#what-is-tai-e)
- [How to Obtain Runnable Jar of Tai-e?](#how-to-obtain-runnable-jar-of-tai-e)
- [How to Include Tai-e in Your Project?](#how-to-include-tai-e-in-your-project)
    - [Stable Version](#stable-version)
    - [Latest Version](#latest-version)
- [Documentation](#documentation)
    - [Reference Documentation](#reference-documentation)
    - [Changelog](#changelog)
- [Tai-e Assignments](#tai-e-assignments)

## What is Tai-e?

Tai-e (Chinese: 太阿; pronunciation: [ˈtaɪə:]) is a new static analysis framework for Java (please see our [ISSTA 2023 paper](https://cs.nju.edu.cn/tiantan/papers/issta2023.pdf) for details), which features arguably the "best" designs from both the novel ones we proposed and those of classic frameworks such as Soot, WALA, Doop, and SpotBugs.
Tai-e is easy-to-learn, easy-to-use, efficient, and highly extensible, allowing you to easily develop new analyses on top of it.

Currently, Tai-e provides the following major analysis components (and more analyses are on the
way):

- Powerful pointer analysis framework
  - On-the-fly call graph construction
  - Various classic and advanced techniques of heap abstraction and context sensitivity for pointer analysis
  - Extensible analysis plugin system (allows to conveniently develop and add new analyses that interact with pointer analysis)
- Configurable security analysis
  - Taint analysis, which allows to configure sources, sinks, taint transfers, and sanitizers
  - Detection of various information leakages and injection vulnerabilities
  - Various precision and efficiency tradeoffs (benefit from the pointer analysis framework)
- Various fundamental/utility analyses
  - Fundamental analyses, e.g., reflection analysis and exception analysis
  - Modern language feature analyses, e.g., lambda and method reference analysis, and invokedynamic analysis
  - Utility tools like analysis timer, constraint checker (for debugging), and various graph dumpers
- Control/Data-flow analysis framework
  - Control-flow graph construction
  - Classic data-flow analyses, e.g., live variable analysis, constant propagation
  - Your data-flow analyses
- SpotBugs-like bug detection system
  - Bug detectors, e.g., null pointer detector, incorrect `clone()` detector
  - Your bug detectors

Tai-e is developed in Java, and it can run on major operating systems including Windows, Linux, and macOS.

As a courtesy to the developers, we expect that you **please [cite](CITATION.bib) the paper** from ISSTA 2023 describing the Tai-e framework in your research work:

Tian Tan and Yue Li. 2023.
**Tai-e: A Developer-Friendly Static Analysis Framework for Java by Harnessing the Good Designs of Classics.**
In Proceedings of the 32nd ACM SIGSOFT International Symposium on Software Testing and Analysis (ISSTA '23), July 17–21, 2023, Seattle, WA, USA ([pdf](https://cs.nju.edu.cn/tiantan/papers/issta2023.pdf), [bibtex](CITATION.bib)).

## How to Obtain Runnable Jar of Tai-e?
The simplest way is to download it from [GitHub Releases](https://github.com/pascal-lab/Tai-e/releases).

Alternatively, you might build the latest Tai-e yourself from the source code. This can be simply accomplished via Gradle (be sure that Java 17 (or higher version) is available on your system).
You just need to run command `gradlew fatJar`, and then the runnable jar will be generated in `tai-e/build/`, which includes Tai-e and all its dependencies.

## How to Include Tai-e in Your Project?
Tai-e is designed as a standalone tool, but you also have the option to include it in your project as a dependency.
It is available on Maven repositories, allowing you to easily integrate it into your Java projects using build tools such as Gradle and Maven.
We maintain both stable and latest versions of Tai-e, and here are the corresponding coordinates in Gradle and Maven script formats:

### Stable Version
For Gradle:

```kotlin
dependencies {
    implementation("net.pascal-lab:tai-e:0.2.2")
}
```

For Maven:

```xml

<dependencies>
    <dependency>
        <groupId>net.pascal-lab</groupId>
        <artifactId>tai-e</artifactId>
        <version>0.2.2</version>
    </dependency>
</dependencies>
```

### Latest Version

For Gradle:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    implementation("net.pascal-lab:tai-e:0.5.1-SNAPSHOT")
}
```

For Maven:

```xml
<repositories>
    <repository>
        <id>snapshots</id>
        <name>Sonatype snapshot server</name>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.pascal-lab</groupId>
        <artifactId>tai-e</artifactId>
        <version>0.5.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

You can use these coordinates in your Gradle or Maven scripts to include the desired version of Tai-e in your project.

## Documentation

### Reference Documentation

We have provided detailed information of Tai-e in the [Reference Documentation](https://tai-e.pascal-lab.net/docs/current/reference/en/index.html), which covers various aspects such as [Setup in IntelliJ IDEA](https://tai-e.pascal-lab.net/docs/current/reference/en/setup-in-intellij-idea.html), [Command-Line Options](https://tai-e.pascal-lab.net/docs/current/reference/en/command-line-options.html), and [Development of New Analysis](https://tai-e.pascal-lab.net/docs/current/reference/en/develop-new-analysis.html).

Please note that the reference documentation mentioned above pertains to *the latest version* of Tai-e.
If you need documentation for a specific stable version, please refer to the [Documentation Index](https://tai-e.pascal-lab.net/docs).
Additionally, the documentation is included within the repository and maintained alongside the source code.
You can access the reference documentation for a particular version of Tai-e (in AsciiDoc format) by exploring the [docs/en](docs/en) directory, starting from [index.adoc](docs/en/index.adoc).
This allows you to access version-specific documentation for Tai-e.

In addition to the reference
documentation, [Javadocs](https://tai-e.pascal-lab.net/docs/current/api/index.html) for Tai-e are
also available as a useful reference resource.

### Changelog
Since we are actively developing and updating Tai-e, we record the notable changes we made, especially the new features and breaking changes, in [CHANGELOG](CHANGELOG.md).
If you find something wrong after updating Tai-e, maybe you could check [CHANGELOG](CHANGELOG.md) for useful information.

## Tai-e Assignments
In addition, we have developed an [educational version of Tai-e](https://tai-e.pascal-lab.net/en/intro/overview.html) where eight programming assignments are carefully designed for systematically training learners to implement various static analysis techniques to analyze real Java programs.
The educational version shares a large amount of code with Tai-e, thus doing the assignments would be a good way to get familiar with Tai-e.
