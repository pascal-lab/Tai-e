# Changelog

## [Unreleased] - 2026-06-10

### New Features
- New frontend for converting input Java programs to Tai-e IR.
  - Integrate our new frontend (OOPSLA'25), which is significantly faster and more reliable than the previous Soot-based frontend.
  - Add option `--ssa` to enable SSA IR generation.
  - Support parsing Java programs up to Java 25.
  - Preserve source-level variable names in generated Tai-e IR when they are available in bytecode.
  - Add option `--jre-dir` to specify the JRE directory for the Java library selected by `-java`.
- Add Android analysis plugin.
  - Integrate PacDroid (ICSE'25), our pointer-analysis-centric framework for Android apps, into Tai-e's pointer analysis.
  - Add option `--android-mode` to enable Android mode.
  - Add option `--android-jars` to specify the Android platform jars required for Android analysis.
  - Model key Android framework semantics, including lifecycle/callback behaviors, inter-component communication (ICC), and other common Android features such as intent extras and asynchronous execution.
- Add Spring DI and WEC analysis plugin.
  - Add pointer-analysis option `spring` to enable the built-in Spring Framework analysis plugin.
  - Model Spring dependency injection (DI) by collecting bean definitions from configured annotations, `@Bean` factory methods, and XML bean configurations.
  - Simulate Spring-managed constructor, field, and method injection, including qualifier/name-based and type-based bean resolution.
  - Collect Spring Web endpoint configurations (WEC) from controller and request-mapping annotations, and add endpoint handlers as entry points with receiver beans and mock parameters.
  - Provide `spring-plugin-config.yaml` to configure supported DI annotations, XML tags, and WEC annotations.
- Add backends in `pascal.taie.backend` for Tai-e IR
  - Add a bytecode backend to convert Tai-e IR into Java bytecode and JAR archives.
  - Add a simple virtual machine to interpret Tai-e IR. This VM is not fully-fledged and is intended mainly for testing purposes.

### Breaking Changes
- Change the default value of option `--world-builder` to
  `pascal.taie.frontend.java.JavaWorldBuilder`, i.e., the new Java frontend.
- Deprecate option `-pp`/`--prepend-JVM`; it is accepted with a warning and will be removed in a future version. When both `-java` and `--jre-dir` are omitted, Tai-e uses the JRE of the current Java runtime as the analyzed library by default.
- Deprecate option `-ap`/`--allow-phantom`; it is accepted with a warning and will be removed in a future version. Tai-e now allows phantom classes by default and reports them with warnings.

## [0.5.2] - 2025-12-31

### New Features
- Add format check for command-line arguments.
- Add performance monitoring (CPU and memory usage) functionality to `Monitor`.
- Pointer analysis
  - Add special handling for zero-length arrays to enhance PTA precision.
  - Add may-alias-pair client to count may-alias variable pairs.
  - Model `Arrays.copyOf` for non-functional arrays for soundness.
  - Model `jdk.internal.misc.Unsafe` to obtain sound results for `ConcurrentHashMap`.
  - Improve the performance of `Obj.getType()` by caching `Obj`'s type.
- Side-effect analysis
  - Improve `SideEffectAnalysis` precision using context-sensitive information and more efficient algorithms.
- Class hierarchy analysis (CHA)
  - Improve `CHABuilder` precision via resolving callees using the type of the receiver variable.

### Fixes
- Fix NPE in Zipper-e pre-analysis.
- Fix the behavior of `UnionFindSet.setCount()` when used concurrently.
- Fix the k-value setup for the CustomEntryPoint testcase.
- Fix `ReflectionAnalysis` plugin to enable `AnnotationModel` only for Java 5+.
- Fix static fields handling in `UnsafeModel` to prevent invalid instance field access.
- Fix the missing `STATIC` modifier of static phantom fields in `ClassHierarchyImpl`.
- Fix NPE in Exception Analysis.
- Fix `SideEffectAnalysis` incorrect behavior when `only-app` is false.
- Fix Mahjong's `FieldPointsToGraph` by handling non-functional `MockObj` in `PointerAnalysisResultImpl`.
- Fix the detection of jar files in `BenchmarkRunner`.
- Fix `CHABuilder` method resolution for static and special methods.
- Fix shadowed instance fields in dumped points-to set.
- Fix missing edges when converting context-sensitive call graph to context-insensitive call graph.
- Escape special characters in `StringLiteral`.

### Contributors

We would like to thank the following community members for their contributions to [this release](https://github.com/pascal-lab/Tai-e/compare/v0.5.1...v0.5.2): [cs-cat](https://github.com/cs-cat), [Jinpeng Wang](https://github.com/jjppp), [FoggyDawn](https://github.com/FoggyDawn), [Zhenyu Yan](https://github.com/Michael1015198808), [RacerZ](https://github.com/RacerZ-fighting), [Chenxi Li](https://github.com/ayanamists), [Teng Zhang](https://github.com/zhangt2333), [Zhiwei Zhang](https://github.com/auroraberry).

## [0.5.1] - 2024-12-31

### New Features
- Add side-effect analysis.
- The options `--class-path` and `--app-class-path` can be repeated multiple times to specify multiple paths.
- Pointer analysis
  - Add `Plugin.onPhaseFinish()`.
  - Support specifying multiple method signatures in one `@InvokeHandler` annotation.
  - Add `getInfo()` to call graph edges and pointer flow edges.
  - Add pointer analysis assertion mechanism to ease testing.
  - Add `pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin`and `IRModelPlugin` to replace original `Model` and `IRModel`, provide more convenient interfaces to support `@InvokeHandler`.
- Taint analysis
  - Support specifying IndexRef (e.g., `index: "0[*]"` and `index: "0.f"`) in call sources and parameter sources.
  - Support specifying IndexRef in sinks.
  - Support interactive mode, allowing users to modify the taint configuration file and re-run taint analysis without needing to re-run the whole program analysis.
  - Enhance TFG dumping by adding taint configuration and call site info to Source/Sink node and TaintTransfer edge.
  - Support programmatic taint config provider.
  - Add commonly used taint configurations.
- Class hierarchy analysis (CHA)
  - Support ignoring call sites that call methods declared in `java.lang.Object`.
  - Support ignoring call sites whose callees exceed given limit.
- Signature pattern and matcher
  - Add `pascal.taie.language.classes.SignatureMatcher` which supports retrieving classes, methods, or fields whose signature match given pattern.
  - Use signature matcher in taint analysis and `@InvokeHandler` to simplify signature configuration.

### Breaking Changes
- API changes
  - Change `Solver.addPFGEdge(Pointer,Pointer,FlowKind,Type)` and `Solver.addPFGEdge(Pointer,Pointer,FlowKind,Transfer)` to `Solver.addPFGEdge(PointerFlowEdge)` and related APIs.
  - Deprecate `pascal.taie.analysis.pta.plugin.util.Model` and `IRModel` (these two interfaces are currently preserved for compatibility, and will be removed in the future).
  - Change `PrimitiveType` from `enum` to an `interface` and implement it by classes that represent concrete primitive types. Refine the types of certain expressions from `PrimitiveType` to the concrete primitive types.

### Fixes
- Fix incorrect classpath argument for the frontend where the `-acp` option is not being used. This issue is only reproducible when `--prepend-JVM` (`-pp`) is set to `true`.
- Fix mismatch between number of parameter names and number of actual parameters in JMethod for inner class.
- Fix option parser, now treat only the first colon as delimiter between a key and a value (before each colon is treated as delimiter).
- Fix empty log file when running via JAR.

### Contributors

We would like to thank the following community members for their contributions to the releases (v0.5.1, v0.2.2) of Tai-e: [Wangxiz](https://github.com/Wangxiz), [Chenghang Shi](https://github.com/enochii), [YaphetsH](https://github.com/YaphetsH), [GnSight](https://github.com/ftyghome), [Zhaohui Wang](https://github.com/chaos-warzh), [cs-cat](https://github.com/cs-cat), [Yinning Xiao](https://github.com/ningninger), [Zhiwei Zhang](https://github.com/auroraberry), [Hengbin Zheng](https://github.com/Isla-top), [Chenxi Zhang](https://github.com/penguinfirst).

## [0.2.2] - 2023-09-23

### New Features
- Add option `--app-class-path`.
- Add option `--keep-results`.
- Add option `--output-dir`.
- Add option `-wc, --world-cache-mode`.
- Add def-use analysis.
- Add dominator-finding algorithm.
- Add generics signature information for Class, Method, and Field.
- Include documentation source in the repository.
- Taint analysis
  - Support taint source for arguments of method calls and method parameters.
  - Support taint source for field loads.
  - Support taint sanitization for method parameters.
  - Dump taint flow graph.
  - Support loading multiple taint configuration files.
  - Support taint transfer between variables and instance fields/array elements.
  - Support call-site mode.
- Pointer analysis
  - Support adding entry points of the program to analyze.
  - Support analysis time limit.
  - Support propagation for values of primitive types.
  - Support hybrid inference-based and log-based reflection analysis.
  - Add Solar reflection analysis (TOSEM'19).
  - Support annotation-based invoke handler registration.
  - Support dumping points-to set in YAML format.

### Breaking Changes
- Option and configuration changes
  - Change All `dump` related options. Previously, most `dump` options require users to specify a path to dump file; now, Tai-e uses fixed path for dump file (the file name is fully fixed, and users can still change dump directory via option `--output-dir`), so that users only need to specify `true` or `false` for all `dump` options.
  - Rename analysis `class-dumper` to `ir-dumper`.
  - Pointer analysis
    - Replace `merge-string-constants` by `distinguish-string-constants`.
    - Replace `action` by `dump` and `expected-file`.
  - Taint analysis
    - Require to add `kind` to *source* configurations. Previously, the taint analysis only supports one kind of sources, i.e., result of method call. Now, we support more kinds of sources, including argument or result of method call (`kind: call`), and method parameter (`kind: param`) , so users need to specify kind of each source. Please see [an example](src/test/resources/pta/taint/taint-config-instance-source-sink.yml).
- API changes
  - Change `pascal.taie.analysis.pta.core.heap.HeapModel.getMockObj(String,...)` to `HeapModel.getMockObj(Descriptor,...)`.
  - Change APIs of `pascal.util.graph.Edge` and its subclasses.
  - Change return type of `Exp.getUses()` to `Set<RValue>`.
  - Change return type of `Stmt.getUses()` to `Set<RValue>`.

## [0.0.3] - 2022-08-02
- First release.


[Unreleased]: https://github.com/pascal-lab/Tai-e/compare/v0.5.2...HEAD
[0.5.2]: https://github.com/pascal-lab/Tai-e/compare/v0.5.1...v0.5.2
[0.5.1]: https://github.com/pascal-lab/Tai-e/compare/v0.2.2...v0.5.1
[0.2.2]: https://github.com/pascal-lab/Tai-e/compare/v0.0.3...v0.2.2
[0.0.3]: https://github.com/pascal-lab/Tai-e/releases/tag/v0.0.3
