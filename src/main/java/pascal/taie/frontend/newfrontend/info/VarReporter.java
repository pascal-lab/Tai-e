package pascal.taie.frontend.newfrontend.info;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;

public class VarReporter {
    static VarReporter instance = new VarReporter();

    final List<Pair<Integer, Integer>> res = new ArrayList<>();

    public static VarReporter get() {
        return instance;
    }

    public void report(int maxLocal, int varSize) {
        synchronized (res) {
            res.add(new Pair<>(maxLocal, varSize));
        }
    }

    public double getRatio() {
        long allMaxLocal = 0;
        long allVarSize = 0;
        for (var i : res) {
            allMaxLocal += i.first();
            allVarSize += i.second();
        }
        return allVarSize / (double) allMaxLocal;
    }
}
