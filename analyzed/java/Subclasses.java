public class Subclasses {
}

interface I {}

interface II {}

interface III extends I, II {}

interface IIII extends III {}

class C {}

class D extends C {}

class E extends C implements I, II {}

class F implements III {}

class G extends E {}
