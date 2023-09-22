package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.Expression;
import pascal.taie.language.classes.JField;

public record FieldInit(JField field, Expression init) implements JavaInit {
}
