package pascal.util;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Hybrid of array and hash set.
 * Small maps are represented as arrays; above a certain threshold a hash set is used instead.
 * Moreover, empty sets and singleton sets are represented with just a reference.
 * Elements cannot be null.
 */
@SuppressWarnings("SuspiciousArrayCast")
public final class HybridArrayHashSet<V> implements Set<V>, Serializable {

    // invariant: at most one of singleton, array and hashset is non-null

    private static final String NULL_KEY = "HybridArrayHashSet does not permit null keys";

    /**
     * Default threshold for the number of items necessary for the array to become a hash set.
     */
    private static final int ARRAY_SIZE = 16;

    /**
     * The singleton value. Null if not a singleton.
     */
    private V singleton;

    /**
     * The array with the items. Null if the array is not used.
     */
    private V[] array;

    /**
     * Counter for the number of items in the container.
     */
    private int number_of_used_array_entries; // = number of non-null entries in array, if non-null

    /**
     * The hash set with the items. Null if the hash set is not used.
     */
    private HashSet<V> hashset;

    /**
     * Constructs a new hybrid set.
     */
    public HybridArrayHashSet() {
        // do nothing
    }

    /**
     * Constructs a new hybrid set from the given collection.
     */
    @SuppressWarnings("unchecked")
    public HybridArrayHashSet(Collection<V> m) {
        int m_size = m.size();
        if (m_size == 0)
            return;
        if (m_size == 1) {
            singleton = getFirstElement(m);
        } else if (m_size <= ARRAY_SIZE) {
            array = (V[]) new Object[ARRAY_SIZE];
            number_of_used_array_entries = 0;
            boolean m_is_set = m instanceof Set;
            outer: for (V v : m) {
                if (v == null)
                    throw new NullPointerException(NULL_KEY);
                if (!m_is_set) { // avoid duplicates
                    for (int i = 0; i < number_of_used_array_entries; i++) {
                        if (array[i].equals(v)) {
                            continue outer;
                        }
                    }
                }
                array[number_of_used_array_entries++] = v;
            }
        } else {
            for (V v : m)
                if (v == null)
                    throw new NullPointerException(NULL_KEY);
            hashset = new HashSet<>(m);
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V getFirstElement(Collection<V> m) {
        if (!(m instanceof HybridArrayHashSet<?>))
            return m.iterator().next();
        HybridArrayHashSet<?> set = (HybridArrayHashSet<?>) m;
        if (set.singleton != null)
            return (V) set.singleton;
        if (set.array != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (set.array[i] != null)
                    return (V) set.array[i];
            return null;
        }
        if (set.hashset != null)
            return (V) set.hashset.iterator().next();
        return null;
    }

    @Override
    public boolean add(V e) {
        if (e == null)
            throw new NullPointerException(NULL_KEY);
        if (singleton != null) {
            if (singleton.equals(e))
                return false;
            convertSingletonToArray();
        }
        if (array != null) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array[i] != null && array[i].equals(e))
                    return false;
            }
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] == null) {
                    array[i] = e;
                    number_of_used_array_entries++;
                    return true;
                }
            convertArrayToHashSet();
        }
        if (hashset != null)
            return hashset.add(e);
        singleton = e;
        return true;
    }

    @SuppressWarnings("unchecked")
    private void convertSingletonToArray() {
        array = (V[]) new Object[ARRAY_SIZE];
        array[0] = singleton;
        number_of_used_array_entries = 1;
        singleton = null;
    }

    private void convertArrayToHashSet() {
        hashset = new HashSet<>(ARRAY_SIZE * 2);
        for (int i = 0; i < ARRAY_SIZE; i++)
            if (array[i] != null)
                hashset.add(array[i]);
        array = null;
        number_of_used_array_entries = 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(@Nonnull Collection<? extends V> c) {
        int c_size = c.size();
        if (c_size == 0)
            return false;
        int max_new_size = c_size + size();
        if (array == null && hashset == null && max_new_size == 1) {
            V v = getFirstElement(c);
            if (v == null)
                throw new NullPointerException(NULL_KEY);
            singleton = v;
            return true;
        }
        if (array == null && hashset == null && max_new_size <= ARRAY_SIZE) {
            if (singleton != null)
                convertSingletonToArray();
            else {
                array = (V[]) new Object[ARRAY_SIZE];
                number_of_used_array_entries = 0;
            }
        }
        if (array != null && max_new_size <= ARRAY_SIZE) {
            boolean changed = false;
            int next = 0;
            loop:
            for (V e : c) {
                if (e == null)
                    throw new NullPointerException(NULL_KEY);
                for (int i = 0; i < ARRAY_SIZE; i++)
                    if (array[i] != null && array[i].equals(e))
                        continue loop;
                while (array[next] != null)
                    next++;
                array[next++] = e;
                number_of_used_array_entries++;
                changed = true;
            }
            return changed;
        }
        for (V v : c)
            if (v == null)
                throw new NullPointerException(NULL_KEY);
        if (array != null) {
            convertArrayToHashSet();
        }
        if (hashset == null) {
            hashset = new HashSet<>(ARRAY_SIZE + max_new_size);
        }
        if (singleton != null) {
            hashset.add(singleton);
            singleton = null;
        }
        return hashset.addAll(c);
    }

    @Override
    public void clear() {
        if (singleton != null)
            singleton = null;
        else if (array != null) {
            Arrays.fill(array, null);
            number_of_used_array_entries = 0;
        } else if (hashset != null)
            hashset.clear();
    }

    @Override
    public boolean contains(Object o) {
        if (singleton != null)
            return singleton.equals(o);
        if (array != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] != null && array[i].equals(o))
                    return true;
            return false;
        }
        if (hashset != null)
            return hashset.contains(o);
        return false;
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        for (Object o : c)
            if (!contains(o))
                return false;
        return true;
    }

    @Override
    public boolean isEmpty() {
        if (singleton != null)
            return false;
        if (array != null)
            return number_of_used_array_entries == 0;
        if (hashset != null)
            return hashset.isEmpty();
        return true;
    }

    @Override
    public int size() {
        if (singleton != null)
            return 1;
        if (array != null)
            return number_of_used_array_entries;
        if (hashset != null)
            return hashset.size();
        return 0;
    }

    @Override
    public Iterator<V> iterator() {
        if (singleton != null)
            return new Iterator<V>() {

                boolean done;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public V next() {
                    if (done)
                        throw new NoSuchElementException();
                    done = true;
                    return singleton;
                }

                @Override
                public void remove() {
                    if (done && singleton != null) {
                        singleton = null;
                    } else
                        throw new IllegalStateException();
                }
            };
        if (array != null)
            return new ArrayIterator();
        if (hashset != null)
            return hashset.iterator();
        return new Iterator<V>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public V next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new IllegalStateException();
            }
        };
    }

    @Override
    public boolean remove(Object o) {
        if (singleton != null) {
            if (singleton.equals(o)) {
                singleton = null;
                return true;
            }
            return false;
        }
        if (array != null) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array[i] != null && array[i].equals(o)) {
                    array[i] = null;
                    number_of_used_array_entries--;
                    return true;
                }
            }
            return false;
        }
        if (hashset != null)
            return hashset.remove(o);
        return false;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        boolean changed = false;
        for (Object o : c)
            changed |= remove(o);
        return changed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean changed = false;
        for (Iterator<V> it = iterator(); it.hasNext(); )
            if (!c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        return changed;
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        if (singleton != null) {
            Object[] a = new Object[1];
            a[0] = singleton;
            return a;
        }
        if (array != null) {
            Object[] a = new Object[number_of_used_array_entries];
            int k = 0;
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] != null)
                    a[k++] = array[i];
            return a;
        }
        if (hashset != null)
            return hashset.toArray();
        return new Object[0];
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        if (singleton != null) {
            if (a.length < 1)
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
            a[0] = (T) singleton; // TODO: throw ArrayStoreException if not T :> V
            return a;
        }
        if (array != null) {
            if (a.length < number_of_used_array_entries)
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), number_of_used_array_entries);
            int k = 0;
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] != null)
                    a[k++] = (T) array[i]; // TODO: throw ArrayStoreException if not T :> V
            while (k < a.length)
                a[k++] = null;
            return a;
        }
        if (hashset != null)
            //noinspection SuspiciousToArrayCall
            return hashset.toArray(a);
        Arrays.fill(a, null);
        return a;
    }

    @Override
    public int hashCode() { // see contract for Set.hashCode
        if (singleton != null)
            return singleton.hashCode();
        if (array != null) {
            int h = 0;
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] != null)
                    h += array[i].hashCode();
            return h;
        }
        if (hashset != null)
            return hashset.hashCode();
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Set<?>))
            return false;
        Set<?> s = (Set<?>) obj;
        if (size() != s.size())
            return false;
        // TODO: special support for singletons...
        if (hashCode() != s.hashCode())
            return false;
        return containsAll(s);
    }

    @Override
    public String toString() {
        if (singleton != null)
            return "[" + singleton + ']';
        if (array != null) {
            StringBuilder b = new StringBuilder();
            b.append('[');
            boolean first = true;
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array[i] != null) {
                    if (first)
                        first = false;
                    else
                        b.append(", ");
                    b.append(array[i]);
                }
            b.append(']');
            return b.toString();
        }
        if (hashset != null)
            return hashset.toString();
        return "[]";
    }

    private final class ArrayIterator implements Iterator<V> {

        int next, last;

        ArrayIterator() {
            findNext();
            last = -1;
        }

        private void findNext() {
            while (next < ARRAY_SIZE && array[next] == null)
                next++;
        }

        @Override
        public boolean hasNext() {
            return next < ARRAY_SIZE;
        }

        @Override
        public V next() {
            if (next == ARRAY_SIZE)
                throw new NoSuchElementException();
            last = next;
            V v = array[next++];
            findNext();
            return v;
        }

        @Override
        public void remove() {
            if (last == -1 || array[last] == null)
                throw new IllegalStateException();
            array[last] = null;
            number_of_used_array_entries--;
            findNext();
        }
    }
}
