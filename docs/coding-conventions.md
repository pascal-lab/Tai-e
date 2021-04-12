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


### Annotation (@Override, @Nullable, ...)
Always add `@Override` annotation for overridden methods.

For the methods that may return `null`, add `@Nullable` annotation to their return values. For example, `public @Nullable X getX()`.

### Use Tai-e Library
- Use `CollectionUtils` to Create Sets/Maps.
When creating Set/Map, use proper `CollectionUtils.newSet/newMap()` factory methods instead of `new HashSet/Map<>()`.

- Use `HashUtils.hash()` to Compute Hash Value of Multiple Objects. If the arguments may be `null`, use `HashUtils.safeHash()`.

- Obtain String Constants from `StringReps`.
When using JDK class names, method subsignatures or signaturess, refer to corresponding fields of `StringReps`.
