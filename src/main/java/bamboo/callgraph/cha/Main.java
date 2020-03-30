package bamboo.callgraph.cha;

import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);

        // Configure transformer
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.cha", new CHACallGraphBuilder()));

        // Run main analysis
        soot.Main.main(args);
    }
}
