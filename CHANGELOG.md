# Changelog

## [Unreleased] - 2025-02-16

### New Features
- Add performance sampler to record CPU and memory usage during execution.
- Add format check for command-line arguments.
- Pointer analysis
  - Add special handling for zero-length arrays to enhance PTA precision.
  - Add may-alias-pair client to count may-alias variable pairs.

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


[Unreleased]: https://github.com/pascal-lab/Tai-e/compare/v0.5.1...HEAD
[0.5.1]: https://github.com/pascal-lab/Tai-e/compare/v0.2.2...v0.5.1
[0.2.2]: https://github.com/pascal-lab/Tai-e/compare/v0.0.3...v0.2.2
[0.0.3]: https://github.com/pascal-lab/Tai-e/releases/tag/v0.0.3
