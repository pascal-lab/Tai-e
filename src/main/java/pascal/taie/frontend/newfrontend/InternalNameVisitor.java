package pascal.taie.frontend.newfrontend;

import java.util.Collection;

public class InternalNameVisitor {
    public static void visitDescriptor(String descriptor, Collection<String> container) {
        char first = descriptor.charAt(0);
        if (first == 'L') {
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            container.add(internalName);
        } else if (first == '[') {
            extractArrayType(descriptor, container);
        } else if (first == '(') {
            int i = 1;
            while (descriptor.charAt(i) != ')') {
                char now = descriptor.charAt(i);
                switch (now) {
                    case 'B', 'C', 'D', 'I', 'F', 'J', 'S', 'Z' -> {
                        i++;
                    }
                    case '[' -> {
                        while (descriptor.charAt(i) == '[') {
                            i++;
                        }
                        if (descriptor.charAt(i) == 'L') {
                            i = readNext(descriptor, container, i);
                        } else {
                            i++;
                        }
                    }
                    case 'L' -> {
                        i = readNext(descriptor, container, i);
                    }
                    default -> throw new UnsupportedOperationException();
                }
            }
            i++;
            if (descriptor.charAt(i) == 'L') {
                String internalName = descriptor.substring(i + 1, descriptor.length() - 1);
                container.add(internalName);
            }
        }
    }

    public static void visitInternalName(String internalName, Collection<String> containers) {
        if (internalName.charAt(0) == '[') {
            extractArrayType(internalName, containers);
        } else {
            containers.add(internalName);
        }
    }

    private static void extractArrayType(String internalName, Collection<String> containers) {
        int i = 0;
        while (internalName.charAt(i) == '[') {
            i++;
        }
        if (internalName.charAt(i) == 'L') {
            String res = internalName.substring(i + 1, internalName.length() - 1);
            containers.add(res);
        }
    }

    private static int readNext(String descriptor, Collection<String> container, int i) {
        i++;
        int start = i;
        while (descriptor.charAt(i) != ';') {
            i++;
        }
        String internalName = descriptor.substring(start, i);
        container.add(internalName);
        i++;
        return i;
    }
}
