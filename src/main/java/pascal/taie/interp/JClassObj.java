package pascal.taie.interp;

import pascal.taie.language.type.ClassType;

public class JClassObj extends JObject {

    public JClassObj(ClassType type) {
        super(type);
    }

    @Override
    public String toString() {
        return "JClassObj: [" + this.getType() + "]";
    }
}
