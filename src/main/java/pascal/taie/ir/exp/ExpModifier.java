package pascal.taie.ir.exp;

import pascal.taie.language.type.Type;

/**
 * Attribute modifier only for frontend. Clients should not use this modifier unless
 * they fully understand the consequence.
 */
public class ExpModifier {

    /**
     * For Var.
     */
    public static void setName(Var var, String name) {
        var.setName(name);
    }

    public static void setType(Var var, Type type) {
        var.setType(type);
    }

    public static void setIndex(Var var, int index) {
        var.setIndex(index);
    }
}
