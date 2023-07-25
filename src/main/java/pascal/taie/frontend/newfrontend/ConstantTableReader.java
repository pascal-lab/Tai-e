package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Type;
import pascal.taie.util.collection.Sets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConstantTableReader {
    public static final int HEAD = 0xcafebabe;
    // Constant pool types
    public static final byte CONSTANT_Utf8 = 1;
    public static final byte CONSTANT_Integer = 3;
    public static final byte CONSTANT_Float = 4;
    public static final byte CONSTANT_Long = 5;
    public static final byte CONSTANT_Double = 6;
    public static final byte CONSTANT_Class = 7;
    public static final byte CONSTANT_String = 8;
    public static final byte CONSTANT_FieldRef = 9;
    public static final byte CONSTANT_MethodRef = 10;
    public static final byte CONSTANT_InterfaceMethodRef = 11;
    public static final byte CONSTANT_NameAndType = 12;
    public static final byte CONSTANT_MethodHandle = 15;
    public static final byte CONSTANT_MethodType = 16;
    public static final byte CONSTANT_InvokeDynamic = 18;
    public static final byte CONSTANT_Module = 19;
    public static final byte CONSTANT_Package = 20;

    private List<String> binaryNames;

    public ConstantTableReader() {
    }

    public List<String> read(byte[] content) {
        parse(ByteBuffer.wrap(content));
        return binaryNames;
    }

    void parse(ByteBuffer buf) {
        binaryNames = new ArrayList<>();
        if (buf.order(ByteOrder.BIG_ENDIAN).getInt() != HEAD) {
            throw new FrontendException("not a valid class file");
        }

        // minor
        buf.getChar();
        // version
        buf.getChar();
        int count = buf.getChar();
        String[] constants = new String[count];
        Set<Integer> internals = Sets.newSet(count / 2);
        Set<Integer> descriptors = Sets.newSet(count / 2);
        for (int ix = 1; ix < count; ix++) {
            int index1, index2;
            byte tag = buf.get();
            switch (tag) {
                default -> throw new FrontendException("unknown pool item type " + buf.get(buf.position() - 1));
                case CONSTANT_Utf8 -> {
                    String str = decodeString(buf);
                    constants[ix] = str;
                }
                case CONSTANT_Class -> {
                    index1 = buf.getChar();
                    internals.add(index1);
                }
                case CONSTANT_MethodType -> {
                    index1 = buf.getChar();
                    descriptors.add(index1);
                }
                case CONSTANT_FieldRef, CONSTANT_MethodRef, CONSTANT_InterfaceMethodRef -> {
                    index1 = buf.getChar();
                    buf.getChar();
                    internals.add(index1);
                }
                case CONSTANT_NameAndType -> {
                    buf.getChar();
                    index2 = buf.getChar();
                    descriptors.add(index2);
                }
                case CONSTANT_Double, CONSTANT_Long -> {
                    buf.getLong();
                    ix++;
                }
                case CONSTANT_MethodHandle -> {
                    buf.get();
                    buf.getChar();
                }
                case CONSTANT_InvokeDynamic -> {
                    buf.getChar();
                    buf.getChar();
                }
                case CONSTANT_Integer -> buf.getInt();
                case CONSTANT_Float -> buf.getFloat();
                case CONSTANT_Module, CONSTANT_Package, CONSTANT_String -> buf.getChar();
            }
        }

        internals.forEach(i -> addInternalName(constants[i]));
        descriptors.forEach(i -> addDescriptor(constants[i]));
    }

    private void addBinaryName(String binaryName) {
        binaryNames.add(binaryName);
    }

    private void addDescriptor(String descriptor) {
        addType(Type.getType(descriptor));
    }

    private void addInternalName(String internalName) {
        if (internalName == null) {
            return;
        }
        addType(Type.getObjectType(internalName));
    }

    private void addType(Type t) {
        if (t.getSort() == Type.ARRAY) {
            addType(t.getElementType());
        } else if (t.getSort() == Type.OBJECT) {
            addBinaryName(t.getClassName());
        } else if (t.getSort() == Type.METHOD) {
            for (var i : t.getArgumentTypes()) {
                addType(i);
            }
            addType(t.getReturnType());
        }
    }

    // TODO: optimize
    private static String decodeString(ByteBuffer buf) {
        int size = buf.getChar(), oldLimit = buf.limit();
        buf.limit(buf.position() + size);
        StringBuilder sb = new StringBuilder(size + (size >> 1));
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b > 0)
                sb.append((char) b);
            else {
                int b2 = buf.get();
                if ((b & 0xf0) != 0xe0)
                    sb.append((char) ((b & 0x1F) << 6 | b2 & 0x3F));
                else {
                    int b3 = buf.get();
                    sb.append((char) ((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F));
                }
            }
        }
        buf.limit(oldLimit);
        return sb.toString();
    }
}
