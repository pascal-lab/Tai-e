## License Header

## Naming

## Format

## Coding
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

### Use `CollectionUtils` to Create Sets/Maps
When creating Set/Map, use proper `CollectionUtils.newSet/newMap()` factory methods instead of `new HashSet/Map<>()`.

### Use `HashUtils.hash()` to Compute Hash Value of Multiple Objects


### Output (use Logger)


### Annotation (@Override, @Nullable, ...)
Always add `@Override` annotation for overridden methods.

For the methods that may return `null`, add `@Nullable` annotation to their return values. For example, `public @Nullable X getX()`.
