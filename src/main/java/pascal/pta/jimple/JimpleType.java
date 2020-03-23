package pascal.pta.jimple;

import pascal.pta.element.Type;
import soot.RefType;
import soot.SootClass;

class JimpleType implements Type {

    private soot.Type sootType;
    private SootClass sootClass;

    JimpleType(soot.Type sootType) {
        this.sootType = sootType;
        if (sootType instanceof RefType) {
            // TODO - handle array type
            this.sootClass = ((RefType) sootType).getSootClass();
        }
    }

    SootClass getSootClass() {
        return sootClass;
    }

    soot.Type getSootType() {
        return sootType;
    }

    @Override
    public String getName() {
        return sootType.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleType that = (JimpleType) o;
        return sootType.equals(that.sootType);
    }

    @Override
    public int hashCode() {
        return sootType.hashCode();
    }

    @Override
    public String toString() {
        return sootType.toString();
    }
}
