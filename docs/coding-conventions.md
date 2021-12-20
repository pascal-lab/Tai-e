## License Header

## Naming

## Style (TODO: use style tool)
### Import
- Wildcard imports (`import x.y.*`) are not used.
- Imports should be sorted (use `Ctrl+Alt+o` in IntelliJ IDEA)

## Coding
### Minimal Accessibility
Always use the access modifiers with minimal accessibility for classes/methods/fields, e.g., if an inner-class/method/field is not used by any other classes, use `private` modifier.

### Use `final` Field
When possible, declare `final` fields.

### Return Stream
```
class Graph {
    private Collection<Node> nodes;

    Stream<Node> nodes() { return nodes.stream(); } // preferred

    Collection<Node> getNodes() { return nodes; }
}
```

### Output (use Logger)


### Annotation (@Override, @Nullable, @Nonnull, ...)
Always add `@Override` annotation for overridden methods.

For the methods that may return `null`, add `@Nullable` annotation to their return values. For example, `public @Nullable X getX()`.

For the methods that require non-`null` arguments, add `@Nonnull` annotation to the specific parameters, For example, `void setX(@Nonnull x)`.

### Use Tai-e Library
- Use `Sets`/`Maps` to Create Sets/Maps.
When creating Set/Map, use proper `Sets.newSet`/`Maps.newMap()` factory methods instead of `new HashSet/Map<>()`.

- Tai-e provides some data structures (in package `pascal.tai.util.collection`) that are commonly-used in static analysis but not included in JDK, e.g., `MultiMap` and `TwoKeyMap`. You could use them to make life easier.

- Use `Hashes.hash()` to Compute Hash Value of Multiple Objects. If the arguments may be `null`, use `Hashes.safeHash()`.

- Obtain String Constants from `StringReps`.
When using JDK class names, method subsignatures or signaturess, refer to corresponding fields of `StringReps`.
