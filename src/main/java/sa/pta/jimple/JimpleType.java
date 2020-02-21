package sa.pta.jimple;

import sa.pta.element.Type;
import soot.SootClass;

class JimpleType implements Type {

    private soot.Type type;
    private SootClass sootClass;

    @Override
    public String getName() {
        return type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleType that = (JimpleType) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    SootClass getSootClass() {
        return sootClass;
    }
}
