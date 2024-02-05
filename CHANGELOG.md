# Changelog

## [Unreleased] - 2024-01-20

### New Features
- Add side-effect analysis.
- The options `--class-path` and `--app-class-path` can be repeated multiple times to specify multiple paths.
- Pointer analysis
  - Add `Plugin.onPhaseFinish()`.
  - Support specifying multiple method signatures in one `@InvokeHandler` annotation.
  - Add `getInfo()` to call graph edges and pointer flow edges.
  - Add pointer analysis assertion mechanism.
  - Add `pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin`and `IRModelPlugin` to replace original `Model` and `IRModel`, provide more convenient interfaces to support `@InvokeHandler`.
- Taint analysis
  - Support specifying IndexRef (e.g., `index: "0[*]"` and `index: "0.f"`) in call sources and parameter sources.
  - Support specifying IndexRef in sinks.

### Breaking Changes
- API changes
  - Change `Solver.addPFGEdge(Pointer,Pointer,FlowKind,Type)` and `Solver.addPFGEdge(Pointer,Pointer,FlowKind,Transfer)` to `Solver.addPFGEdge(PointerFlowEdge)` and related APIs.
  - Deprecate `pascal.taie.analysis.pta.plugin.util.Model` and `IRModel` (these two interfaces are currently preserved for compatibility, and will be removed in the future).
  - Change `PrimitiveType` from `enum` to an `interface` and implement it by classes that represent concrete primitive types. Refine the types of certain expressions from `PrimitiveType` to the concrete primitive types.

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


[Unreleased]: https://github.com/pascal-lab/Tai-e/compare/v0.2.2...HEAD
[0.2.2]: https://github.com/pascal-lab/Tai-e/compare/v0.0.3...v0.2.2
[0.0.3]: https://github.com/pascal-lab/Tai-e/releases/tag/v0.0.3
