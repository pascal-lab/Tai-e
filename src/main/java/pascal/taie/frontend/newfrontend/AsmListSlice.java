package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import pascal.taie.frontend.newfrontend.dbg.BytecodeVisualizer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A slice of {@link InsnList}
 */
public class AsmListSlice implements List<AbstractInsnNode> {

    private final InsnList list;

    /**
     * inclusive
     */
    private final int start;

    public int getEnd() {
        return end;
    }

    /**
     * exclusive
     */
    private final int end;

    public AsmListSlice(InsnList list, int start, int end) {
        this.list = list;
        this.start = start;
        this.end = end;
    }

    @Override
    public int size() {
        int preSize = end - start;
        return Math.max(preSize, 0);
    }

    @Override
    public boolean isEmpty() {
        return start >= end;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public Iterator<AbstractInsnNode> iterator() {
        return new Iterator<>() {
            int cursor = start;

            @Override
            public boolean hasNext() {
                return cursor < end;
            }

            @Override
            public AbstractInsnNode next() {
                return list.get(cursor++);
            }
        };
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(AbstractInsnNode abstractInsnNode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends AbstractInsnNode> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, @Nonnull Collection<? extends AbstractInsnNode> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractInsnNode get(int index) {
        return list.get(index + start);
    }

    @Override
    public AbstractInsnNode set(int index, AbstractInsnNode element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, AbstractInsnNode element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractInsnNode remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public ListIterator<AbstractInsnNode> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public ListIterator<AbstractInsnNode> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public List<AbstractInsnNode> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        List<String> instr = new ArrayList<>();
        for (AbstractInsnNode insn : this) {
            instr.add(BytecodeVisualizer.printInsn(insn));
        }
        return String.join("\n", instr);
    }

    public int getStart() {
        return start;
    }
}
