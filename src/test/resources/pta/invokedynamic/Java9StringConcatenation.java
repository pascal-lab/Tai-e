public class Java9StringConcatenation {

    static boolean b = true;
    static byte by = 6;
    static short sh = 6;
    static char ch = 'a';
    static int i = 6;
    static float fl = 6.0f;
    static long l = 6;
    static double d = 6.0d;
    static String s = "foo";
    static String sNull = null;
    static Object o = "bar";
    static Object oNull = null;
    static CharSequence cs = "bar";
    static MyClass myCl = new MyClass();
    static MyClassNull myClNull = new MyClassNull();
    static Object myCl2 = new MyClass();

    public static void main(String[] args) throws Exception {
        stringBoolean();
        stringByte();
        stringShort();
        stringChar();
        stringInt();
        stringLong();
        stringFloat();
        stringDouble();
        stringstring();
        stringStringNull();
        stringObject();
        stringObject2();
        stringObject3();
        stringObjectNull();
        stringObjectNull2();
        stringCharSeq();
        stringObjectCharSeq();
        stringObjectCharSeqInt();
        constConst();
        constStringConstString();
        stringConstStringConst();
        mix();
    }

    static String stringBoolean() {
        return s + b;
    }

    static String stringByte() {
        return s + by;
    }

    static String stringShort() {
        return s + sh;
    }

    static String stringChar() {
        return s + ch;
    }

    static String stringInt() {
        return s + i;
    }

    static String stringLong() {
        return s + l;
    }

    static String stringFloat() {
        return s + fl;
    }

    static String stringDouble() {
        return s + d;
    }

    static String stringstring() {
        return s + s;
    }

    static String stringStringNull() {
        return s + sNull;
    }

    static String stringObject() {
        return s + o;
    }

    static String stringObject2() {
        return s + myCl;
    }

    static String stringObject3() {
        return s + myCl2;
    }

    static String stringObjectNull() {
        return s + oNull;
    }

    static String stringObjectNull2() {
        return s + myClNull;
    }

    static String stringCharSeq() {
        return s + cs;
    }

    static String stringObjectCharSeq() {
        return s + o + cs;
    }

    static String stringObjectCharSeqInt() {
        return s + o + cs + i;
    }

    static String constConst() {
        return "aaa" + "bbb";
    }

    static String constStringConstString() {
        return "aaa" + s + "bbb" + s;
    }

    static String stringConstStringConst() {
        return s + "aaa" + s + "bbb";
    }

    static String mix() {
        return s + "aaa" + 666 + s + 2333l + s + 3.14159 + "bbb";
    }

    static class MyClass {
        public String toString() {
            return "MyClass";
        }
    }

    static class MyClassNull {
        public String toString() {
            return null;
        }
    }

}
