package bamboo.dataflow.analysis.constprop;

import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_prepend_classpath(true);

        // Configure transformer
        PackManager.v()
                .getPack("jtp")
                .add(new Transform("jtp.constprop", ConstantPropagation.v()));

        // Run main analysis
        soot.Main.main(args);
    }

}
