public class Recur {
    public static void main(String[] args) {
        dfs(4);
    }

    public static int dfs(int n) {
        if (n < 0) return 1;
        else return bfs(n-1)+1;
    }

    public static int bfs(int n) {
        if (n < 0) return 1;
        else return dfs(n-1)+1;
    }
}