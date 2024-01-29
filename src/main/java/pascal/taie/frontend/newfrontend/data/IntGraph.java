package pascal.taie.frontend.newfrontend.data;

public class IntGraph extends SparseArray<IntList> {
    public IntGraph(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected IntList createInstance() {
        return new IntList(4);
    }

    public void addEdge(int from, int to) {
        get(from).add(to);
    }

}
