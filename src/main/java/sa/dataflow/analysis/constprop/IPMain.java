package sa.dataflow.analysis.constprop;

import sa.callgraph.cha.CHACallGraphBuilder;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class IPMain {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_whole_program(true);

        // Configure transformer
        Pack wjtp = PackManager.v().getPack("wjtp");
        wjtp.add(new Transform("wjtp.cha", new CHACallGraphBuilder()));
        wjtp.add(new Transform("wjtp.constprop", new IPConstantPropagation()));

        // Run main analysis
        soot.Main.main(args);
    }
}
