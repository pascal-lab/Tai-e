package sa.dataflow.analysis.deadcode;

import sa.dataflow.analysis.constprop.ConstantPropagation;
import sa.dataflow.analysis.livevar.LiveVariableAnalysis;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);

        // Configure transformer
        Pack jtp = PackManager.v().getPack("jtp");
        jtp.add(new Transform("jtp.constprop", ConstantPropagation.v()));
        jtp.add(new Transform("jtp.livevar", LiveVariableAnalysis.v()));
        jtp.add(new Transform("jtp.deadcode", DeadCodeElimination.v()));

        // Run main analysis
        soot.Main.main(args);
    }
}
