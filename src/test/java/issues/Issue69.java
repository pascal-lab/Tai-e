package issues;

import org.junit.jupiter.api.Test;

/**
 * This class provides a reproducible test case for
 * <a href="https://github.com/pascal-lab/Tai-e/issues/69">Issue #69</a>.
 * <p>
 * The analyzed program and related resources, such as 'taint-config.yml',
 * are located in the 'src/test/resources/issues/69' directory.
 * <p>
 * If you are analyzing the binary, you can include '.class' files or '.jar'
 * files in the above directory.
 */
class Issue69 {

    /**
     * The test method that invokes the main method with specific parameters for analysis.
     * It sets the classpath, module, and analysis options.
     * <p>
     * The example does not include any assertion but a comment that
     * tells the developer what to check manually next.
     */
    @Test
    void test() {
        pascal.taie.Main.main(
                "-cp", "src/test/resources/issues/69",
                "-m", "Main",
                "-a", """
                      pta=cs:ci;
                      implicit-entries:true;
                      distinguish-string-constants:null;
                      reflection-inference:solar;
                      taint-config:src/test/resources/issues/69/taint-config.yml;
                      """
        );
        // and then check the 'taint-flow-graph.dot' file in 'output' directory
    }

}
