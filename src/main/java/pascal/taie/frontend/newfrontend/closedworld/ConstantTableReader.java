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

package pascal.taie.frontend.newfrontend.closedworld;

import pascal.taie.frontend.newfrontend.exception.ClassFileInfo;
import pascal.taie.frontend.newfrontend.exception.ConstantTableCorruption;
import pascal.taie.frontend.newfrontend.exception.CorruptClassFileException;
import pascal.taie.frontend.newfrontend.main.TaiePhase;
import pascal.taie.project.DotClassFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Read constant table from class file.
 * Some code is taken from the asm library.</p>
 *
 * <p>The implementation mainly focus on efficiency,
 * contains some tricky code</p>
 */
public class ConstantTableReader {
    private static final int HEAD = 0xcafebabe;
    // Constant pool types
    private static final byte CONSTANT_Utf8 = 1;
    private static final byte CONSTANT_Integer = 3;
    private static final byte CONSTANT_Float = 4;
    private static final byte CONSTANT_Long = 5;
    private static final byte CONSTANT_Double = 6;
    private static final byte CONSTANT_Class = 7;
    private static final byte CONSTANT_String = 8;
    private static final byte CONSTANT_FieldRef = 9;
    private static final byte CONSTANT_MethodRef = 10;
    private static final byte CONSTANT_InterfaceMethodRef = 11;
    private static final byte CONSTANT_NameAndType = 12;
    private static final byte CONSTANT_MethodHandle = 15;
    private static final byte CONSTANT_MethodType = 16;
    private static final byte CONSTANT_InvokeDynamic = 18;
    private static final byte CONSTANT_Module = 19;
    private static final byte CONSTANT_Package = 20;
    private static final byte[] RUNTIME_VISIBLE_ANNOTATIONS_UTF8 = {
            (byte) 0x52, (byte) 0x75, (byte) 0x6e, (byte) 0x74, (byte) 0x69, (byte) 0x6d,
            (byte) 0x65, (byte) 0x56, (byte) 0x69, (byte) 0x73, (byte) 0x69, (byte) 0x62,
            (byte) 0x6c, (byte) 0x65, (byte) 0x41, (byte) 0x6e, (byte) 0x6e, (byte) 0x6f,
            (byte) 0x74, (byte) 0x61, (byte) 0x74, (byte) 0x69, (byte) 0x6f, (byte) 0x6e,
            (byte) 0x73
    };

    private List<String> internalNames;

    private int[] constantsOffset;
    private boolean[] internalsLoad;
    private boolean[] descriptorsLoad;
    private char[] decodeBuffer;

    private final byte[] classFileBuffer;

    private int offset = 0;

    /**
     * internal name of the target class
     */
    private final String internalName;

    private final DotClassFile file;

    public ConstantTableReader(String internalName, DotClassFile file, byte[] content) {
        this.internalName = internalName;
        this.file = file;
        classFileBuffer = content;
    }

    public List<String> read() throws CorruptClassFileException {
        parse();
        return internalNames;
    }

    private int readUnsignedShort() {
        byte[] classBuffer = classFileBuffer;
        return ((classBuffer[offset++] & 0xFF) << 8) | (classBuffer[offset++] & 0xFF);
    }

    private int readUnsignedShortPure(int offset) {
        byte[] classBuffer = classFileBuffer;
        return ((classBuffer[offset] & 0xFF) << 8) | (classBuffer[offset + 1] & 0xFF);
    }

    private int readInt() {
        byte[] classBuffer = classFileBuffer;
        return ((classBuffer[offset++] & 0xFF) << 24)
                | ((classBuffer[offset++] & 0xFF) << 16)
                | ((classBuffer[offset++] & 0xFF) << 8)
                | (classBuffer[offset++] & 0xFF);
    }

    private boolean arrayEqualsPure(byte[] target, int offset, int len) {
        if (len != target.length) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (classFileBuffer[offset + i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    private void parse() throws CorruptClassFileException {
        internalNames = new ArrayList<>();
        // head
        offset += 4;
        // minor
        offset += 2;
        // version
        offset += 2;
        int count = readUnsignedShort();
        constantsOffset = new int[count];
        internalsLoad = new boolean[count];
        descriptorsLoad = new boolean[count];
        int maxLen = 0;
        for (int ix = 1; ix < count; ix++) {
            int index1, index2;
            byte tag = classFileBuffer[offset++];
            switch (tag) {
                case CONSTANT_Utf8 -> {
                    constantsOffset[ix] = offset;
                    int len = readUnsignedShort();
                    maxLen = Math.max(len, maxLen);
                    offset += len;
                }
                case CONSTANT_Class -> {
                    // CONSTANT_Class_info {
                    //    u1 tag;
                    //    u2 name_index;
                    // }
                    index1 = readUnsignedShort();
                    internalsLoad[index1] = true;
                }
                case CONSTANT_MethodType -> {
                    index1 = readUnsignedShort();
                    descriptorsLoad[index1] = true;
                }
                case CONSTANT_FieldRef, CONSTANT_MethodRef, CONSTANT_InterfaceMethodRef,
                     CONSTANT_InvokeDynamic -> {
                    // E.g.
                    // CONSTANT_Fieldref_info {
                    //    u1 tag;
                    //    u2 class_index;          ;; Points to CONSTANT_Class_info
                    //    u2 name_and_type_index;  ;; Points to CONSTANT_NameAndType_info
                    // }
                    offset += 4;
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
                case CONSTANT_Integer, CONSTANT_Float -> offset += 4;
                case CONSTANT_Module, CONSTANT_Package, CONSTANT_String -> offset += 2;

                default -> throw new CorruptClassFileException(TaiePhase.CLOSED_WORLD_ANALYSIS,
                        new ClassFileInfo(file), new ConstantTableCorruption(offset,
                        String.format("invalid constant table tag: 0x%02X", tag)));
            }
        }

        decodeBuffer = new char[maxLen];
        offset += 6;
        int interfaceCount =  readUnsignedShort();
        offset += interfaceCount * 2;
        readFieldOrMethod(descriptorsLoad);
        readFieldOrMethod(descriptorsLoad);
        parseAttributes();

        // Now parse descriptors and internal names
        for (int i = 1; i < count; ++i) {
            if (internalsLoad[i]) {
                visitInternalName(constantsOffset[i], internalNames);
            } else if (descriptorsLoad[i]) {
                visitDescriptor(constantsOffset[i], internalNames);
            }
        }
    }

    private void readFieldOrMethod(boolean[] descriptors) throws CorruptClassFileException {
        int methodCount = readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            offset += 4;
            descriptors[readUnsignedShort()] = true;
            parseAttributes();
        }
    }

    private void parseAttributes() throws CorruptClassFileException {
        int attributesCount = readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            int nameIndex = readUnsignedShort();
            int attributeLength = readInt();

            int nameOffset = constantsOffset[nameIndex];
            int nameLength = readUnsignedShortPure(nameOffset);
            if (arrayEqualsPure(RUNTIME_VISIBLE_ANNOTATIONS_UTF8, nameOffset + 2, nameLength)) {
                parseAnnotationContent();  // Parse annotation content
            } else {
                offset += attributeLength; // Skip other attributes
            }
        }
    }

    /**
     * <p>Precondition</p>
     * <code>offset</code> points to the <code>num_annotations;</code> of annotation attribute
     */
    private void parseAnnotationContent() throws CorruptClassFileException {
        int numAnnotations = readUnsignedShort();
        for (int i = 0; i < numAnnotations; i++) {
            parseAnnotation();
        }
    }

    private void parseAnnotation() throws CorruptClassFileException {
        int typeIndex = readUnsignedShort();  // Class descriptor
        descriptorsLoad[typeIndex] = true;

        int numElementValuePairs = readUnsignedShort();
        for (int j = 0; j < numElementValuePairs; j++) {
            readUnsignedShort();  // Ignore element name
            parseElementValue();  // Parse element value and check for class references
        }
    }

    private void parseElementValue() throws CorruptClassFileException {
        byte tag = classFileBuffer[offset++];
        switch (tag) {
            case 'B': case 'C': case 'D': case 'F': case 'I': case 'J': case 'S': case 'Z':
                offset += 2; // primitive type, just skip
                break;
            case 's':
                offset += 2; // skip strings for now
                break;
            case 'c': // class type
                int ix = readUnsignedShort();
                descriptorsLoad[ix] = true;
                break;
            case 'e':
                offset += 4;
                break;
            case '@': // Nested annotation
                parseAnnotation(); // Recursive call for nested annotations
                break;
            case '[': // Array of values
                int arraySize = readUnsignedShort();
                for (int i = 0; i < arraySize; i++) {
                    parseElementValue(); // Parse each element in the array
                }
                break;
            default:
                throw new CorruptClassFileException(TaiePhase.CLOSED_WORLD_ANALYSIS,
                        new ClassFileInfo(file), new ConstantTableCorruption(offset,
                        String.format("invalid annotation element tag: 0x%02X", tag)));
        }
    }

    /**
     * Decode string at position offset.
     * Now only used for debug purpose
     */
    private String decodeString(int offset) {
        char[] charBuffer = decodeBuffer;
        int currentOffset = offset;
        int len = readUnsignedShortPure(currentOffset);
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
        return new String(charBuffer, 0, strLength);
    }

    // These fields are used to parse descriptors and internal names
    private int currentOffset;
    private int decodeOffset;

    private void visitDescriptor(int offset, Collection<String> container) {
        currentOffset = offset + 2;
        decodeOffset = 0;
        char first = nextChar();
        if (first == 'L') {
            extractAndAddInternalName(container);
        } else if (first == '[') {
            extractArrayType(container);
        } else if (first == '(') {
            char now;
            now = nextChar();
            while (now != ')') {
                switch (now) {
                    case 'B', 'C', 'D', 'I', 'F', 'J', 'S', 'Z' -> {
                    }
                    case '[' -> {
                        extractArrayType(container);
                    }
                    case 'L' -> {
                        extractAndAddInternalName(container);
                    }
                    default -> throw new UnsupportedOperationException();
                }
                now = nextChar();
            }
            if (nextChar() == 'L') {
                String internalName = extractInternalName();
                container.add(internalName);
            }
        }
    }

    /**
     * The input (Content of <code>CONSTANT_Class_info</code>) is either
     * <ul>
     *     <li>Real internal name, e.g. <code>java/lang/Thread</code></li>
     *     <li>Array Descriptor, e.g. <code>[Ljava/lang/Thread;</code></li>
     * </ul>
     */
    private void visitInternalName(int offset, Collection<String> container) {
        int len = readUnsignedShortPure(offset);
        currentOffset = offset + 2;
        int start = currentOffset;
        decodeOffset = 0;
        char c = nextChar();
        if (c == '[') {
            extractArrayType(container);
        } else {
            while (currentOffset < len + start) {
                nextChar();
            }
            container.add(new String(decodeBuffer, 0, decodeOffset));
        }
    }

    private void extractAndAddInternalName(Collection<String> container) {
        String internalName = extractInternalName();
        container.add(internalName);
    }

    private String extractInternalName() {
        int startOffset = decodeOffset;
        while (nextChar() != ';') {
        }
        String str = new String(decodeBuffer, startOffset, decodeOffset - startOffset - 1);
        assert !str.endsWith(";");
        return str;
    }

    private void extractArrayType(Collection<String> container) {
        char curr;
        do {
            curr = nextChar();
        } while (curr == '[');
        if (curr == 'L') {
            String res = extractInternalName();
            container.add(res);
        }
    }

    private char nextChar() {
        int currentByte = classFileBuffer[currentOffset++];
        char now;
        if ((currentByte & 0x80) == 0) {
            now = (char) (currentByte & 0x7F);
        } else if ((currentByte & 0xE0) == 0xC0) {
            now = (char) (((currentByte & 0x1F) << 6) + (classFileBuffer[currentOffset++] & 0x3F));
        } else {
            now = (char) (((currentByte & 0x0F) << 12) + ((classFileBuffer[currentOffset++] & 0x3F) << 6)
                    + (classFileBuffer[currentOffset++] & 0x3F));
        }
        decodeBuffer[decodeOffset++] = now;
        return now;
    }
}
