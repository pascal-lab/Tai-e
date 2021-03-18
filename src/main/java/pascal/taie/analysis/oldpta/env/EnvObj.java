/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.oldpta.env;

import pascal.taie.analysis.oldpta.ir.AbstractObj;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;

import java.util.Objects;
import java.util.Optional;

/**
 * Objects managed/created by Java runtime environment.
 */
public class EnvObj extends AbstractObj {

    /**
     * Description of this object.
     */
    private final String name;
    private final JMethod containerMethod;

    public EnvObj(String name, Type type, JMethod containerMethod) {
        super(type);
        this.name = name;
        this.containerMethod = containerMethod;
    }

    @Override
    public Kind getKind() {
        return Kind.ENV;
    }

    @Override
    public String getAllocation() {
        return name;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.ofNullable(containerMethod);
    }

    @Override
    public Type getContainerType() {
        // TODO: set a better container type?
        return containerMethod != null
                ? containerMethod.getDeclaringClass().getType()
                : type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvObj envObj = (EnvObj) o;
        return name.equals(envObj.name)
                && Objects.equals(containerMethod, envObj.containerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, containerMethod);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Env]");
        if (containerMethod != null) {
            sb.append(containerMethod).append("/");
        }
        sb.append(name);
        return sb.toString();
    }
}
