# Changelog

## [Unreleased] - 2023-03-29

### New Features

- Add option `--app-class-path`.
- Add option `--keep-results`.
- Add option `--output-dir`.
- Add Def-use analysis.
- Add Dominator-finding algorithm.
- Taint analysis
  - Support taint source for arguments of method call and method parameters.
  - Support taint sanitization for method parameters.
  - Dump taint flow graph.
  - Support to load multiple taint configuration files.
- Pointer analysis
  - Support to add entry points of the program to analyze.
  - Support analysis time limit.
  - Support propagation for values of primitive types.
  - Support hybrid inference-based and log-based reflection analysis.
  - Add Solar reflection analysis.
  - Support annotation-based invoke handler registration.

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


## [0.0.3] - 2022-08-02
- First release.
