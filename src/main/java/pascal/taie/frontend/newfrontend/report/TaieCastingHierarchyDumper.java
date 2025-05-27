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

package pascal.taie.frontend.newfrontend.report;

import pascal.taie.frontend.newfrontend.Utils;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.PhiStmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;


public class TaieCastingHierarchyDumper {

    public static boolean isPhantomRelated(TaieCastingInfo info) {
        Set<ClassType> typeSet = Sets.newSet();
        for (TypeDefs def : getTaieCastingContext(info).defs()) {
            getJClass(def.type()).ifPresent((c) -> {
                Set<ClassType> upperClosure = Utils.upperClosure(c.getType());
                typeSet.addAll(upperClosure);
            });
        }

        return typeSet.stream().anyMatch((t) -> t.getJClass().isPhantom());
    }

    private static String getClassHierarchyForCasting(TaieCastingInfo info) {
        List<JClass> classes = new ArrayList<>();
        getJClass(info.leftType()).ifPresent((l) -> {
            classes.add(l);
            getJClass(info.rightType()).ifPresent(classes::add);
        });
        TaieCastingContext context = getTaieCastingContext(info);
        for (TypeDefs def : context.defs()) {
            getJClass(def.type()).ifPresent(classes::add);
        }
        return new ClassHierarchyTree(classes).toDotFile();
    }

    public static void writeCastingToDot(Path parent, TaieCastingInfo info) {
        Path out = parent.resolve(info.method().getName() + ".dot");
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
        IR ir = info.method().getIR();
        MultiMap<Var, Stmt> varDefs = Maps.newMultiMap();
        MultiMap<Catch, Type> catchTypes = Maps.newMultiMap();
        for (Stmt stmt : ir.getStmts()) {
            if (stmt instanceof DefinitionStmt<?, ?> def) {
                if (def.getLValue() instanceof Var var) {
                    varDefs.put(var, stmt);
                }
            }
            if (stmt.getUses().contains(info.var())) {
                uses.add(new TypeConstraint(stmt, null));
            }
        }
        for (ExceptionEntry entry : ir.getExceptionEntries()) {
            Var var = entry.handler().getExceptionRef();
            varDefs.put(var, entry.handler());
            catchTypes.put(entry.handler(), entry.catchType());
        }
        Queue<Var> queue = new LinkedList<>();
        queue.add(info.var());
        Set<Var> visited = Sets.newSet();
        while (!queue.isEmpty()) {
            Var var = queue.poll();
            if (visited.contains(var)) {
                continue;
            }
            visited.add(var);
            Set<Stmt> def = varDefs.get(var);
            for (Stmt stmt : def) {
                if (stmt instanceof DefinitionStmt<?, ?> defStmt) {
                    if (stmt instanceof PhiStmt frontendPhiStmt) {
                        for (RValue v : frontendPhiStmt.getRValue().getUses()) {
                            if (v instanceof Var var1) {
                                queue.add(var1);
                            }
                        }
                    } else {
                        defs.add(new TypeDefs(stmt, defStmt.getRValue().getType()));
                    }
                }
                if (stmt instanceof Catch catchStmt) {
                    for (Type type : catchTypes.get(catchStmt)) {
                        defs.add(new TypeDefs(stmt, type));
                    }
                }
            }
        }

        return new TaieCastingContext(info, defs, uses);
    }

    public static Optional<JClass> getJClass(Type t) {
        if (t instanceof ClassType ct) {
            return Optional.of(ct.getJClass());
        } else {
            return Optional.empty();
        }
    }

//    public static void analysisForBoom() {
//        Set<JClass> affected = new HashSet<>();
//        List<String> inputClasses = World.get().getOptions().getInputClasses();
//        Set<String> input = new HashSet<>(inputClasses);
//        World.get().getClassHierarchy().allClasses().forEach((c) -> {
//           if (c.getSuperClass() == null
//                   || affected.contains(c)
//                   || !input.contains(c.getName())) {
//               return;
//           }
//           Set<JClass> directUpper = new HashSet<>();
//           directUpper.add(c.getSuperClass());
//           directUpper.addAll(c.getInterfaces());
//
//           Set<JClass> children = directUpper.stream()
//                   .filter(c1 -> c1.getType() != Utils.getObject())
//                   .flatMap((c1) -> World.get().getClassHierarchy().getAllSubclassesOf(c1)
//                           .stream())
//                   .collect(Collectors.toSet());
//
//           for (JClass child : children) {
//               if (child == c) {
//                   continue;
//               }
//               Set<ReferenceType> lca = lcaWithoutObj(Set.of(c.getType(), child.getType()));
//               if (lca.size() <= 1) {
//                   continue;
//               }
//               Set<ReferenceType> lca2 = lcaWithoutObj(lca);
//               if (lca2.size() >= 2) {
//                   affected.add(c);
//                   affected.add(child);
//                   affected.addAll(lca.stream().map(TaieCastingReporter::getJClass).map(Optional::get).toList());
//                   affected.addAll(lca2.stream().map(TaieCastingReporter::getJClass).map(Optional::get).toList());
//               }
//           }
//       });
//       System.out.println("3-crown size: " + affected.size());
//    }
//
//    private static Set<ReferenceType> lcaWithoutObj(Set<ReferenceType> in) {
//        return Utils.lca(in).stream().filter(c -> c != Utils.getObject())
//                .collect(Collectors.toSet());
//    }
}

