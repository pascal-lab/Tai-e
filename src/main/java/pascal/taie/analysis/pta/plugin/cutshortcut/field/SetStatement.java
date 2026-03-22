package pascal.taie.analysis.pta.plugin.cutshortcut.field;

import pascal.taie.ir.proginfo.FieldRef;

import java.util.Objects;

/**
 * {@link SetStatement}表示需要向前去寻找callSite，并把这种抽象的Set行为作用到callSite处的指针（参数）的语句
 * 和之前的SetStmt不同，现在的{@link SetStatement}并不一定是简单的{@link pascal.taie.ir.stmt.StoreField}语句
 * 举个例子，如果Set方法内包含两层调用callSite(outer) -> set (包含callSite-inner) -> doSet方法
 * 那么在doSet方法内部存在一个{@link SetStatement}，即为简单的{@link pascal.taie.ir.stmt.StoreField}语句
 * 但是当我们从这个{@link SetStatement}出发，找到了set方法中的callSite-inner后，我们发现这个{@link SetStatement}抽象到callSite-inner后的base和rhs
 * 仍然是**set**方法中的parameter，于是callSite-inner也被抽象成一个{@link SetStatement}，继续类似的操作（寻找callSite），
 * 直到找到一个callSite，使得对应的base和rhs不再是方法的参数，此时建立**abstractStoreField，而不是{@link SetStatement}
 * 具体的逻辑在solver中体现
 * {@param baseIndex} 表示SetStatement的base变量对应的parameter index（因为他一定是方法的参数）
 * {@param fieldRef} 该SetStatement对应的field
 * {@param rhsIndex} 与baseIndex类似
 */
public record SetStatement(ParameterIndex baseIndex, FieldRef fieldRef, ParameterIndex rhsIndex) {
    @Override
    public String toString() {
        return "[SetStmt]" + baseIndex().toString() + "." + fieldRef.getName() + " = " + rhsIndex.toString();
    }
}
