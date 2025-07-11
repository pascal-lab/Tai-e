package pascal.taie.frontend.newfrontend.report;

import pascal.taie.World;

public class StackMergeReporter {
    long totalBlocks;

    long pessimisticBlocks;

    long pessimisticPhis;

    long pessimisticLivePhis;

    private static StackMergeReporter instance;

    static {
        World.registerResetCallback(() -> {
            instance = null;
        });
    }

    public void reportStats(long totalBlocks, long pessimisticBlocks,
                            long pessimisticPhis, long pessimisticLivePhis) {
        this.totalBlocks += totalBlocks;
        this.pessimisticBlocks += pessimisticBlocks;
        this.pessimisticPhis += pessimisticPhis;
        this.pessimisticLivePhis += pessimisticLivePhis;
    }

    public void showStats() {
        System.out.println("total basic blocks:         " + totalBlocks  + "\n" +
                           "pessimistic basic blocks:   " + pessimisticBlocks + ", " + (double) pessimisticBlocks / totalBlocks +"\n" +
                           "                            " + pessimisticPhis + ", " + pessimisticLivePhis
                );
    }

    public static StackMergeReporter get() {
        if (instance == null) {
            instance = new StackMergeReporter();
        }
        return instance;
    }
}
