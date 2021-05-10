package pascal.taie.analysis.bugfinder.dataflow;

import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.Collections;
import java.util.Map;

public class IsNullFact extends MapFact<Var, IsNullValue> {
    public IsNullFact() {
        this(Collections.emptyMap());
    }

    private IsNullFact(Map<Var, IsNullValue> map) {
        super(map);
    }

    private boolean isValid = true;

    private IsNullConditionDecision decision = null;

    @Override
    public IsNullValue get(Var var) {
        return map.getOrDefault(var, IsNullValue.undefValue());
    }

    @Override
    public boolean update(Var key, IsNullValue value) {
        if (key.getType() instanceof ClassType || key.getType() instanceof ArrayType) {
            return super.update(key, value);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public IsNullFact copy() {
        return new IsNullFact(this.map);
    }

    public IsNullConditionDecision getDecision() {
        return decision;
    }

    public void setDecision(IsNullConditionDecision decision) {
        this.decision = decision;
    }

    public void downgradeOnControlSplit() {
        map.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isNullOnSomePath())
            .forEach(entry -> entry.setValue(IsNullValue.nullOnComplexPathValue()));
//            forEach((var, value) -> {
//            if (value.isNullOnSomePath()) {
//                update(var, IsNullValue.nullOnComplexPathValue());
//            }
//        });
    }

    public void setInvalid() {
        map.clear();
        isValid = false;
    }

    public void setValid() { isValid = true;}

    public boolean isValid() {
        return isValid;
    }
}
