package pascal.taie.frontend.newfrontend;

import java.util.ArrayList;
import java.util.List;

/**
 * Read constant table from class file.
 * Some code is taken from asm library.
 */
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

    private final byte[] classFileBuffer;

    private int offset = 0;

    public ConstantTableReader(byte[] content) {
        classFileBuffer = content;
    }

    public List<String> read() {
        parse();
        return binaryNames;
    }

    public int readUnsignedShort() {
        byte[] classBuffer = classFileBuffer;
        return ((classBuffer[offset++] & 0xFF) << 8) | (classBuffer[offset++] & 0xFF);
    }

    public int readInt() {
        byte[] classBuffer = classFileBuffer;
        return ((classBuffer[offset++] & 0xFF) << 24)
                | ((classBuffer[offset++] & 0xFF) << 16)
                | ((classBuffer[offset++] & 0xFF) << 8)
                | (classBuffer[offset++] & 0xFF);
    }

    void parse() {
        binaryNames = new ArrayList<>();
        // head
        offset += 4;
        // minor
        offset += 2;
        // version
        offset += 2;
        int count = readUnsignedShort();
        String[] constants = new String[count];
        boolean[] internalsLoad = new boolean[count];
        boolean[] descriptorsLoad = new boolean[count];
        char[] maxBuffer = new char[classFileBuffer.length];
        for (int ix = 1; ix < count; ix++) {
            int index1, index2;
            byte tag = classFileBuffer[offset++];
            switch (tag) {
                default -> throw new FrontendException("unknown pool item type");
                case CONSTANT_Utf8 -> {
                    String str = decodeString(maxBuffer);
                    constants[ix] = str;
                }
                case CONSTANT_Class -> {
                    index1 = readUnsignedShort();
                    internalsLoad[index1] = true;
                }
                case CONSTANT_MethodType -> {
                    index1 = readUnsignedShort();
                    descriptorsLoad[index1] = true;
                }
                case CONSTANT_FieldRef, CONSTANT_MethodRef, CONSTANT_InterfaceMethodRef -> {
                    index1 = readUnsignedShort();
                    offset += 2;
                    internalsLoad[index1] = true;
                }
                case CONSTANT_NameAndType -> {
                    offset += 2;
                    index2 = readUnsignedShort();
                    descriptorsLoad[index2] = true;
                }
                case CONSTANT_Double, CONSTANT_Long -> {
                    offset += 8;
                    ix++;
                }
                case CONSTANT_MethodHandle -> {
                    offset += 3;
                }
                case CONSTANT_InvokeDynamic -> {
                    offset += 4;
                }
                case CONSTANT_Integer, CONSTANT_Float -> offset += 4;
                case CONSTANT_Module, CONSTANT_Package, CONSTANT_String -> offset += 2;
            }
        }

        offset += 6;
        int interfaceCount =  readUnsignedShort();
        offset += interfaceCount * 2;
        readFieldOrMethod(descriptorsLoad);
        readFieldOrMethod(descriptorsLoad);
        for (int i = 0; i < count; ++i) {
            if (internalsLoad[i]) {
                addInternalName(constants[i]);
            }
            if (descriptorsLoad[i]) {
                addDescriptor(constants[i]);
            }
        }
    }

    private void readFieldOrMethod(boolean[] descriptors) {
        int methodCount = readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            offset += 4;
            descriptors[readUnsignedShort()] = true;
            int attrCount = readUnsignedShort();
            for (int j = 0; j < attrCount; j++) {
                offset += 2;
                int len = readInt();
                offset += len;
            }
        }
    }

    private void addDescriptor(String descriptor) {
        InternalNameVisitor.visitDescriptor(descriptor, binaryNames);
    }

    private void addInternalName(String internalName) {
        if (internalName == null) {
            return;
        }
        InternalNameVisitor.visitInternalName(internalName, binaryNames);
    }


    private String decodeString(char[] charBuffer) {
        // TODO: avoid substring allocation
        // here we don't allocate any string except the final one
        // but during addDescriptor/addInternalName, we allocate a lot of strings
        int currentOffset = this.offset;
        int len = readUnsignedShort();
        currentOffset += 2;
        int endOffset = currentOffset + len;
        int strLength = 0;
        byte[] classBuffer = this.classFileBuffer;
        while (currentOffset < endOffset) {
            int currentByte = classBuffer[currentOffset++];
            if ((currentByte & 0x80) == 0) {
                charBuffer[strLength++] = (char) (currentByte & 0x7F);
            } else if ((currentByte & 0xE0) == 0xC0) {
                charBuffer[strLength++] =
                        (char) (((currentByte & 0x1F) << 6) + (classBuffer[currentOffset++] & 0x3F));
            } else {
                charBuffer[strLength++] =
                        (char)
                                (((currentByte & 0xF) << 12)
                                        + ((classBuffer[currentOffset++] & 0x3F) << 6)
                                        + (classBuffer[currentOffset++] & 0x3F));
            }
        }
        offset = endOffset;
        return new String(charBuffer, 0, strLength);
    }
}
