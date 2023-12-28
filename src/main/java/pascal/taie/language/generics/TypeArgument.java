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


package pascal.taie.language.generics;

import pascal.taie.util.Experimental;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-TypeArgument">
 * JVM Spec. 4.7.9.1 TypeArgument</a>
 */
public final class TypeArgument implements Serializable {

    private static final TypeArgument ALL =
            new TypeArgument(Kind.ALL, null);

    private final Kind kind;

    @Nullable
    private final ReferenceTypeGSignature gSig;

    private TypeArgument(Kind kind,
                         @Nullable ReferenceTypeGSignature gSig) {
        this.kind = kind;
        this.gSig = gSig;
    }

    @Experimental
    public Kind getKind() {
        return kind;
    }

    @Nullable
    @Experimental
    public ReferenceTypeGSignature getGSignature() {
        return gSig;
    }

    public static TypeArgument all() {
        return ALL;
    }

    public static TypeArgument of(char symbol,
                                  @Nonnull ReferenceTypeGSignature gSig) {
        Kind kind = Kind.of(symbol);
        assert kind != Kind.ALL && gSig != null;
        return new TypeArgument(kind, gSig);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case INSTANCEOF -> gSig.toString();
            case ALL -> "?";
            case EXTENDS, SUPER -> "? " + kind.name().toLowerCase() + " " + gSig;
        };
    }

    public enum Kind {

        ALL('*'),

        INSTANCEOF(org.objectweb.asm.signature.SignatureVisitor.INSTANCEOF),
        EXTENDS(org.objectweb.asm.signature.SignatureVisitor.EXTENDS),
        SUPER(org.objectweb.asm.signature.SignatureVisitor.SUPER);

        private final char symbol;

        Kind(char symbol) {
            this.symbol = symbol;
        }

        public static Kind of(char symbol) {
            for (Kind indicator : Kind.values()) {
                if (indicator.symbol == symbol) {
                    return indicator;
                }
            }
            throw new IllegalArgumentException("Unknown wildcard indicator: " + symbol);
        }

    }

}
