package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.TransferGenerator;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeMatching implements TransInferStrategy {

    private static final List<String> taintTypePattern = List.of(
            "String", "Byte", "Char", "Buffer", "Token",
            "Message", "File", "Expression", "Packet", "Request",
            "Cookie", "Session", "Input", "URL", "URI",
            "Query", "Header", "Command", "Payload", "Path",
            "Config", "Environment", "Properties"
    );

    private TransferGenerator generator;

    @Override
    public void setContext(InfererContext context) {
        generator = context.generator();
    }

    @Override
    public Set<InferredTransfer> generate(CSCallSite csCallSite, int index) {
        MethodRef methodRef = csCallSite.getCallSite().getMethodRef();
        Set<InferredTransfer> transfers = Sets.newSet();
        // TODO: add other *to* index
        if (matchType(methodRef.getReturnType())) {
            transfers.addAll(generator.getTransfers(csCallSite, index, InvokeUtils.RESULT));
        }
        if (index != InvokeUtils.BASE
                && !csCallSite.getCallSite().isStatic()
                && matchType(methodRef.getDeclaringClass().getType())) {
            transfers.addAll(generator.getTransfers(csCallSite, index, InvokeUtils.BASE));
        }
        return transfers;
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> matchType(tf.getType()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private String getSimpleTypeName(Type type) {
        String fullName = type.getName();
        int index = fullName.lastIndexOf('.');
        return index == -1 ? fullName : fullName.substring(index + 1);
    }

    private boolean matchType(Type type) {
        String name = getSimpleTypeName(type);
        return taintTypePattern.stream().anyMatch(name::contains);
    }
}
