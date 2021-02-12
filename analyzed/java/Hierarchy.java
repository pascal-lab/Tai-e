public class Hierarchy {
}

interface I {}

interface II {}

interface III extends I, II {}

class C {}

class D extends C {}

class E extends C implements I, II {}

class F implements III {}
