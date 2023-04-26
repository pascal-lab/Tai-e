package pascal.taie.frontend.newfrontend;

import pascal.taie.language.type.Type;

enum Uninitialized implements Type {
    UNINITIALIZED;

    @Override
    public String getName() {
        return "Uninitialized";
    }

}
