package pascal.taie.frontend.newfrontend;

public class UnionFind {
    private final int[] parent;
    public UnionFind(int n) {
        parent = new int[n];
        for (var i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    public int find(int x) {
        if (x == parent[x]) {
            return x;
        }
        // compress the paths
        return parent[x] = find(parent[x]);
    }

    public void union(int x, int y)  {
        var px = find(x);
        var py = find(y);
        if (px != py) {
            parent[px] = py;
        }
    }

    public int size() { // number of groups
        int ans = 0;
        for (int i = 0; i < parent.length; ++ i) {
            if (i == parent[i]) ans ++;
        }
        return ans;
    }
}
