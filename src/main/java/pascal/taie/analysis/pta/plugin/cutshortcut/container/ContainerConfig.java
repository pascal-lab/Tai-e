package pascal.taie.analysis.pta.plugin.cutshortcut.container;

import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.*;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.*;

import java.util.Map;
import java.util.Set;

import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory.MapKey;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.ContExitCategory.MapValue;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.HostKind.*;
import static pascal.taie.analysis.pta.plugin.cutshortcut.container.enums.IterExitCategory.*;

public class ContainerConfig {
    public static ContainerConfig config = new ContainerConfig();

    private static final ClassHierarchy hierarchy = World.get().getClassHierarchy();

    // main modeled collection/map classes
    private final MultiMap<ContainerType, JClass> hostClasses = Maps.newMultiMap();
    // other abstract collection/map classes
    private final MultiMap<ContainerType, JClass> unmodeledClasses = Maps.newMultiMap();

    // Entrance Append method to argument index: m -> k, store for methods like List.add(k), Map.put(k, v)
    private final MultiMap<JMethod, Pair<ContExitCategory, Integer>> EntranceAppendMap = Maps.newMultiMap();

    // Entrance Extend method to argument index: m -> k, store for methods like List.addAll(c), Map.putAll(m)
    private final Map<JMethod, Pair<ExtendType, Integer>> EntranceExtendMap = Maps.newMap();

    // Exit method to category: m -> c, c could be one of [Map-Key, Map-Value, Col-Value]
    private final Map<JMethod, ContExitCategory> ContainerExitMap = Maps.newMap();
    // Exit method - Iterator.previous()/next()/nextElement()
    // Here call-relation between Map.Entry.getKey/getValue() not resolved by Tai-e, hence not modeled here.
    private final Map<JMethod, IterExitCategory> IterExitMap = Maps.newMap();

    private final TwoKeyMap<HostKind, String, HostKind> TransferAPIs = Maps.newTwoKeyMap();
    private final TwoKeyMap<HostKind, String, ContExitCategory> MapEntryExits = Maps.newTwoKeyMap();

    // for m: ArrayList.<init>, add 0 -> -1, meaning array of index 0 copied to base
    private final Map<JMethod, Pair<Integer, Integer>> ArrayInitializer = Maps.newMap();

    private ContainerConfig() {
        initializeTransferAPIs();
    }

    private void initializeTransferAPIs() {
        TransferAPIs.put(COL, "iterator", COL_ITR);
        TransferAPIs.put(COL, "Iterator", COL_ITR);
        TransferAPIs.put(MAP, "entrySet", MAP_ENTRY_SET);
        TransferAPIs.put(MAP, "keySet", MAP_KEY_SET);
        TransferAPIs.put(MAP, "values", MAP_VALUES);
        TransferAPIs.put(MAP, "Entry", MAP_ENTRY);
        TransferAPIs.put(MAP, "keys", MAP_KEY_ITR);
        TransferAPIs.put(MAP_ENTRY_SET, "iterator", MAP_ENTRY_ITR);
        TransferAPIs.put(MAP_VALUES, "iterator", MAP_VALUE_ITR);
        TransferAPIs.put(MAP_KEY_SET, "iterator", MAP_KEY_ITR);
        TransferAPIs.put(MAP_ENTRY_ITR, "next", MAP_ENTRY);

        MapEntryExits.put(MAP_ENTRY, "getValue", MapValue);
        MapEntryExits.put(MAP_ENTRY, "getKey", MapKey);
    }

    public void addIterExitCategory(String methodSig, IterExitCategory category) {
        JMethod method = hierarchy.getMethod(methodSig);
        if (method != null)
            IterExitMap.put(method, category);
    }

    public IterExitCategory getIterExitCategory(JMethod method) { return IterExitMap.getOrDefault(method, null); }

    public boolean isIteratorExitMethods(JMethod method) { return getIterExitCategory(method) == ColIter; }

    public boolean isKeySetClass(Type type) {
        if (type instanceof ClassType classType)
            return hostClasses.get(ContainerType.KEYSET).contains(classType.getJClass());
        return false;
    }

    public boolean isValueSetClass(Type type) {
        if (type instanceof ClassType classType)
            return hostClasses.get(ContainerType.VALUES).contains(classType.getJClass());
        return false;
    }

    public void addEntranceAppendIndex(String methodSig, ContExitCategory category, Integer index) {
        JMethod method = hierarchy.getMethod(methodSig);
        if (method != null)
            EntranceAppendMap.put(method, new Pair<>(category, index));
    }

    public Set<Pair<ContExitCategory, Integer>> getEntranceAppendIndex(JMethod method) { return EntranceAppendMap.get(method); }

    public void addEntranceExtendIndex(String methodSig, ExtendType extendType, Integer index) {
        JMethod method = hierarchy.getMethod(methodSig);
        EntranceExtendMap.put(method, new Pair<>(extendType, index));
    }

    public Pair<ExtendType, Integer> getEntranceExtendIndex(JMethod method) { return EntranceExtendMap.getOrDefault(method, null); }

    public void addContainerExitCategory(String methodSig, ContExitCategory category) {
        JMethod method = hierarchy.getMethod(methodSig);
        if (method != null)
            ContainerExitMap.put(method, category);
    }

    public ContExitCategory getContainerExitCategory(JMethod method) {
        return ContainerExitMap.getOrDefault(method, null);
    }

    public TwoKeyMap<HostKind, String, HostKind> getTransferAPIs() {
        return TransferAPIs;
    }

    public TwoKeyMap<HostKind, String, ContExitCategory> getMapEntryExits() {
        return MapEntryExits;
    }

    public void addHostClass(ContainerType containerType, String clzName) {
        JClass clz = hierarchy.getClass(clzName);
        if (clz != null)
            hostClasses.put(containerType, clz);
    }

    public boolean isHostClass(JClass clz) { return hostClasses.values().contains(clz); }

    public void resolveUnmodeledClasses() {
        hierarchy.getAllSubclassesOf(hierarchy.getClass("java.util.Collection")).forEach(clz -> {
            if (!clz.isAbstract() && !hostClasses.values().contains(clz))
                unmodeledClasses.put(ContainerType.COLLECTION, clz);
        });
        hierarchy.getAllSubclassesOf(hierarchy.getClass("java.util.Map")).forEach(clz -> {
            if (!clz.isAbstract() && !hostClasses.values().contains(clz))
                unmodeledClasses.put(ContainerType.MAP, clz);
        });
    }


    public boolean isUnmodeledClass(Type type) {
        if (type instanceof ClassType classType)
            return unmodeledClasses.values().contains(classType.getJClass());
        return false;
    }

    public void addArrayInitializer(String smethod, int index0, int index1) {
        // index0: index of array variable, index1: index of Collection variable
        JMethod method = hierarchy.getMethod(smethod);
        ArrayInitializer.put(method, new Pair<>(index0, index1));
    }

    public Pair<Integer, Integer> getArrayInitializer(JMethod method) { return ArrayInitializer.get(method); }
}
