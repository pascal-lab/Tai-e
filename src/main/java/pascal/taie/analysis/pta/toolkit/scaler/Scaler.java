/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.toolkit.scaler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultExImpl;
import pascal.taie.analysis.pta.toolkit.util.OAGs;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.graph.Graph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Given a TST (Total Scalability Threshold), select the ST (Scalability Threshold),
 * then select context-sensitivity based on the selected ST value.
 */
public class Scaler {

    private static final Logger logger = LogManager.getLogger(Scaler.class);

    private static final long DEFAULT_TST = 30000000;

    private final PointerAnalysisResultEx pta;

    /**
     * Total scalability threshold.
     */
    private final long tst;

    /**
     * Context computer for the fastest and the most imprecise
     * context sensitivity variant.
     */
    private final ContextComputer bottomLine;

    private final List<ContextComputer> ctxComputers;

    /**
     * Map from a method to total size of points-to sets of all (concerned)
     * variables in the method.
     */
    private final Map<JMethod, Integer> ptsSize = Maps.newMap();

    /**
     * Parses Scaler argument and runs Scaler.
     */
    public static Map<JMethod, String> run(PointerAnalysisResult pta, String arg) {
        long tst;
        if (arg.equals("scaler")) {
            tst = DEFAULT_TST;
        } else if (arg.startsWith("scaler=")) { // scaler=tst
            tst = Integer.parseInt(arg.split("=")[1]);
        } else {
            throw new IllegalArgumentException("Illegal Scaler argument: " + arg);
        }
        return new Scaler(pta, tst).selectContext();
    }

    public Scaler(PointerAnalysisResult ptaBase, long tst) {
        this.pta = new PointerAnalysisResultExImpl(ptaBase, true);
        this.tst = tst;
        bottomLine = new _InsensitiveContextComputer(pta);
        // From the most precise analysis to the least precise analysis
        Graph<Obj> oag = OAGs.build(pta);
        // TODO - make ctxComputers configurable
        ctxComputers = List.of(
                new _2ObjContextComputer(pta, oag),
                new _2TypeContextComputer(pta, oag),
                new _1TypeContextComputer(pta));
    }

    /**
     * Selects context sensitivity variants for the methods in the program.
     * Currently, we only consider instance methods, as the contexts of static
     * methods actually come from instance methods.
     *
     * @return a map from methods to their selected context sensitivity variants.
     */
    public Map<JMethod, String> selectContext() {
        logger.info("Scaler TST: {}", tst);
        Set<JMethod> instanceMethods = pta.getBase()
                .getCallGraph()
                .reachableMethods()
                .filter(m -> !m.isStatic())
                .collect(Collectors.toUnmodifiableSet());
        long st = binarySearch(instanceMethods, tst);
        Map<JMethod, String> csMap = instanceMethods.stream()
                .collect(Collectors.toMap(m -> m, m -> selectVariantFor(m, st)));
        logCSMap(csMap);
        return csMap;
    }

    /**
     * Search the suitable st such that the accumulative size of
     * context-sensitive points to sets of given methods is less than given tst.
     *
     * @return the st for every method
     */
    private long binarySearch(Set<JMethod> methods, long tst) {
        // Select the max value and make it as end
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        long end = methods.stream()
                .mapToLong(m -> getWeight(m, ctxComputers.get(0)))
                .max()
                .getAsLong();
        long start = 0;
        long mid, ret = 0;
        while (start <= end) {
            mid = (start + end) / 2;
            long totalSize = getTotalAccumulativePTS(methods, mid);
            if (totalSize < tst) {
                ret = mid;
                start = mid + 1;
            } else if (totalSize > tst) {
                end = mid - 1;
            } else {
                ret = mid;
                break;
            }
        }
        return ret;
    }

    /**
     * Given a set of methods and a st (scalability threshold),
     * computes the total size of all (concerned) variables in the program.
     */
    private long getTotalAccumulativePTS(Set<JMethod> methods, long st) {
        long total = 0;
        for (JMethod method : methods) {
            if (!isSpecialMethod(method)) {
                // special methods are excluded from this computation
                ContextComputer cc = selectContextComputer(method, st);
                total += getWeight(method, cc);
            }
        }
        return total;
    }

    /**
     * Selects a suitable context computer for given method and st.
     * If there are any ContextComputers which can satisfy that the weight
     * of given method can be less than or equal to given st, then the
     * most expensive (and precise) ContextComputer is returned;
     * otherwise, bottom line is returned.
     *
     * @return the selected context computer for method according to tst
     */
    private ContextComputer selectContextComputer(JMethod method, long st) {
        ContextComputer ctxComp;
        if (isSpecialMethod(method)) {
            // special methods will be analyzed with the most precise variant
            ctxComp = ctxComputers.get(0);
        } else {
            ctxComp = bottomLine;
            for (ContextComputer cc : ctxComputers) {
                if (getWeight(method, cc) <= st) {
                    ctxComp = cc;
                    break;
                }
            }
        }
        return ctxComp;
    }

    /**
     * @return the special methods that should be analyzed with
     * the most precise context sensitivity variant.
     */
    private static boolean isSpecialMethod(JMethod method) {
        return method.getDeclaringClass()
                .getName()
                .startsWith("java.util.");
    }

    /**
     * @return the weight of given method when analyzed using the
     * context sensitivity variant that corresponds to given ContextComputer.
     */
    private long getWeight(JMethod method, ContextComputer cc) {
        return ((long) cc.contextNumberOf(method))
                * ((long) getCIPTSSizeOf(method));
    }

    /**
     * @return total size of points-to sets of all (concerned) variables
     * in given method when analyzed using context insensitivity.
     */
    private int getCIPTSSizeOf(JMethod method) {
        if (!ptsSize.containsKey(method)) {
            int size = method.getIR()
                    .getVars()
                    .stream()
                    .filter(Scaler::isConcerned)
                    .mapToInt(v -> pta.getBase().getPointsToSet(v).size())
                    .sum();
            ptsSize.put(method, size);
        }
        return ptsSize.get(method);
    }

    /**
     * @return if given variable is concerned in pointer analysis.
     */
    private static boolean isConcerned(Var var) {
        Type type = var.getType();
        return type instanceof ReferenceType && !(type instanceof NullType);
    }

    /**
     * Given st, selects suitable context sensitivity variant for given method.
     */
    private String selectVariantFor(JMethod method, long st) {
        ContextComputer ctxComp = selectContextComputer(method, st);
        logger.debug("{}, {}, {}", method,
                ctxComp.getVariantName(), ctxComp.contextNumberOf(method));
        return ctxComp.getVariantName();
    }

    private static void logCSMap(Map<JMethod, String> csMap) {
        if (logger.isDebugEnabled()) {
            csMap.entrySet()
                    .stream()
                    .sorted((e1, e2) -> {
                        int cmp1 = e1.getValue().compareTo(e2.getValue());
                        if (cmp1 != 0) {
                            return cmp1;
                        } else {
                            return e1.getKey().toString()
                                    .compareTo(e2.getKey().toString());
                        }
                    })
                    .forEach(logger::debug);
        }
    }
}
