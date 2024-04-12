package pascal.taie.frontend.newfrontend.report;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class TaieCastingInfoSerializer extends StdSerializer<TaieCastingReporter.TaieCastingInfo> {

    public TaieCastingInfoSerializer() {
        this(null);
    }

    public TaieCastingInfoSerializer(Class<TaieCastingReporter.TaieCastingInfo> t) {
        super(t);
    }

    @Override
    public void serialize(
            TaieCastingReporter.TaieCastingInfo value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        gen.writeStartObject();
        gen.writeStringField("method", value.method().toString());
        gen.writeStringField("stmt", value.stmt().toString());
        gen.writeStringField("leftType", value.leftType().toString());
        gen.writeStringField("var", value.var().toString());
        gen.writeStringField("rightType", value.rightType().toString());
        gen.writeEndObject();
    }
}

class TypeDefsSerializer extends StdSerializer<TaieCastingReporter.TypeDefs> {

    public TypeDefsSerializer() {
        this(null);
    }

    public TypeDefsSerializer(Class<TaieCastingReporter.TypeDefs> t) {
        super(t);
    }

    @Override
    public void serialize(
            TaieCastingReporter.TypeDefs value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        gen.writeStartObject();
        gen.writeStringField("stmt", value.stmt().toString());
        gen.writeStringField("type", value.type().toString());
        gen.writeEndObject();
    }
}

class TypeConstraintSerializer extends StdSerializer<TaieCastingReporter.TypeConstraint> {

    public TypeConstraintSerializer() {
        this(null);
    }

    public TypeConstraintSerializer(Class<TaieCastingReporter.TypeConstraint> t) {
        super(t);
    }

    @Override
    public void serialize(
            TaieCastingReporter.TypeConstraint value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        gen.writeStartObject();
        gen.writeStringField("stmt", value.stmt().toString());
        gen.writeEndObject();
    }
}


public class TaieCastingReporter {
    @JsonSerialize(using = TaieCastingInfoSerializer.class)
    public record TaieCastingInfo(JMethod method, Stmt stmt,
                                         Type leftType, Var var, Type rightType) {
    }

    @JsonSerialize(using = TypeDefsSerializer.class)
    public record TypeDefs(Stmt stmt, Type type) {
    }

    @JsonSerialize(using = TypeConstraintSerializer.class)
    public record TypeConstraint(Stmt stmt, Type type) {
    }

    @JsonSerialize
    public record TaieCastingContext(TaieCastingInfo info, List<TypeDefs> defs, List<TypeConstraint> uses) {
    }

    static {
        World.registerResetCallback(() -> get().castingInfos.clear());
    }

    private static final TaieCastingReporter instance = new TaieCastingReporter();

    private final List<TaieCastingInfo> castingInfos = new ArrayList<>();

    public static TaieCastingReporter get() {
        return instance;
    }

    public void reportCasting(TaieCastingInfo info) {
        castingInfos.add(info);
    }

    public List<TaieCastingInfo> getCastingInfos() {
        return castingInfos;
    }

    public boolean isPhantomRelated(TaieCastingInfo info) {
        Set<ClassType> typeSet = new HashSet<>();
        for (TypeDefs def : getTaieCastingContext(info).defs()) {
            getJClass(def.type()).ifPresent((c) -> {
                Set<ClassType> upperClosure = Utils.upperClosure(c.getType());
                typeSet.addAll(upperClosure);
            });
        }

        return typeSet.stream().anyMatch((t) -> t.getJClass().isPhantom());
    }

    public static String getClassHierarchyForCasting(TaieCastingInfo info) {
        List<JClass> classes = new ArrayList<>();
        getJClass(info.leftType).ifPresent((l) -> {
            classes.add(l);
            getJClass(info.rightType).ifPresent(classes::add);
        });
        IR ir = info.method.getIR();
        for (Stmt stmt : ir.getStmts()) {
            if (stmt instanceof DefinitionStmt<?,?> def) {
                if (def.getLValue() instanceof Var var && var.equals(info.var)) {
                    getJClass(def.getRValue().getType()).ifPresent(classes::add);
                }
            }
        }
        return new ClassHierarchyTree(classes).toDotFile();
    }

    public void writeCastingToDot(Path parent) {
        for (TaieCastingInfo info : castingInfos) {
            writeCastingToDot(parent, info);
        }
    }


    public static void writeCastingToDot(Path parent, TaieCastingInfo info) {
        Path out = parent.resolve(info.method.getName() + ".dot");
        // write to file
        try {
            Files.createDirectories(out.getParent());
            String content = getClassHierarchyForCasting(info);
            Files.write(out, content.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TaieCastingContext getTaieCastingContext(TaieCastingInfo info) {
        List<TypeDefs> defs = new ArrayList<>();
        List<TypeConstraint> uses = new ArrayList<>();
        IR ir = info.method.getIR();
        for (Stmt stmt : ir.getStmts()) {
            if (stmt instanceof DefinitionStmt<?,?> def) {
                if (def.getLValue() instanceof Var var && var.equals(info.var)) {
                    defs.add(new TypeDefs(stmt, def.getRValue().getType()));
                }
            }
            if (stmt.getUses().contains(info.var)) {
                uses.add(new TypeConstraint(stmt, null));
            }
        }
        return new TaieCastingContext(info, defs, uses);
    }

    public void writeCastingToDot() {
        writeCastingToDot(Paths.get("output/casting"));
    }

    public static Optional<JClass> getJClass(Type t) {
        if (t instanceof ClassType ct) {
            return Optional.of(ct.getJClass());
        } else {
            return Optional.empty();
        }
    }
}

