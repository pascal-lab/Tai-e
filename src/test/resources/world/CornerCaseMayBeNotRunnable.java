public class CornerCaseMayBeNotRunnable {
    public static void stackSizeEnlarger(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n) {

    }

    public static void main(String[] args) {
        String WARNING = """
        WARNING: This class is built artificially to expose some corner cases
                 of IRBuilder of newfrontend. So this class might not fit the
                 specification and not runnable.
        The building process is:
                 1. build the bytecode framework from the java source code below;
                 2. insert the bytecodes in the comment to expose some corner cases;
                 3. modify the major version to 45 to escape the Stack Map verification
                    by JVM.

        Source code:
        public class CornerCaseMayBeNotRunnable {
            public static void stackSizeEnlarger(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n) {

            }

            public static void main(String[] args) {
                String WARNING = #THIS WARNING MESSAGE#;
                System.err.println(WARNING);
                stackSizeEnlarger(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14); // To enlarge the stack size of main method.
                int i = Integer.parseInt(args[args.length - 1]);
                int counter = 0;
                // iload_2
                // iconst_5
                // iadd
                // dup // generate temp value
                while (counter < 10) {
                    // iconst_1
                    // iadd // increase the upper temp value in the loop
                    counter += 2;
                }
                // istore 2 // store the temp value being modified into 1 (variable i)
                System.out.println(i);
                // istore 2 // store the temp value NOT being modified into 1 (variable i)
                System.out.println(i); // print the temp value NOT being modified
                throw new RuntimeException(\"From developer: this class is built artificially to expose some corner case, so any result from it is not reliable.\");
            }
        }
        """;
        System.err.println(WARNING);
        stackSizeEnlarger(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
        int i = Integer.parseInt(args[args.length - 1]);
        int counter = 0;
        // iload_2
        // iconst_5 // added
        // iadd     // added
        // dup
        while (counter < 10) {
            // iconst_1
            // iadd
            counter += 2;
        }
        // istore 2 // store the temp value being modified into 1 (variable i)
        System.out.println(i);
        // istore 2 // store the temp value NOT being modified into 1 (variable i)
        System.out.println(i); // print the temp value NOT being modified
        throw new RuntimeException("From developer: this class is built artificially to expose some corner case, so any result from it is not reliable.");
    }
}