package pascal.taie.analysis.bugfinder;

public enum Priorities {

    HIGH_PRIORITY(1),
    NORMAL_PRIORITY(2),
    LOW_PRIORITY(3);

    private final int priority;

    Priorities(int priority) { this.priority = priority; }
//    public static final int LOW_PRIORITY = 3;
//
//    public static final int NORMAL_PRIORITY = 2;
//
//    public static final int HIGH_PRIORITY = 1;
}
