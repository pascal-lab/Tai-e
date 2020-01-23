package sa.icfg;

import java.util.Objects;

public class Edge<Node> {

    public enum Kind {
        LOCAL, // intra-procedural edge
        CALL, // call edge
        RETURN, // return edge
    }

    private final Kind kind;

    private final Node source;

    private final Node target;

    private final int hashCode;

    public Edge(Kind kind, Node source, Node target) {
        this.kind = kind;
        this.source = source;
        this.target = target;
        this.hashCode = Objects.hash(kind, source, target);
    }

    public Kind getKind() {
        return kind;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge<?> edge = (Edge<?>) o;
        return kind == edge.kind &&
                Objects.equals(source, edge.source) &&
                Objects.equals(target, edge.target);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return kind + " Edge{" +  source + " -> " + target + '}';
    }
}
