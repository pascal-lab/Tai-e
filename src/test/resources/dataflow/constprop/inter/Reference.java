public class Reference {
    public static void main(String args[]) {
        Point p = new Point();
        p.x = 2;
        p.y = 3;
        int offset = 1;
        Point p2 = adjustPoint(p, offset);
        int z = p2.x + p2.y;
    }

    public static Point adjustPoint(Point p, int offset) {
        p.x += offset;
        p.y += offset;
        return p;
    }
}

class Point {
    public int x;
    public int y;
}
