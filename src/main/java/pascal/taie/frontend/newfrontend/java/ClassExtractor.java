package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ClassExtractor extends ASTVisitor {

    Stack<ASTNode> outerClasses = new Stack<>();

    List<ASTNode> typeDeclarations = new ArrayList<>();

    Map<ASTNode, ASTNode> outerClassMap = new HashMap<>();

    /**
     * JLS 8. chap. 8.1.3 <br>
     * A statement or expression occurs in a static context if and only if
     * the 'context' (innermost method, constructor, instance initializer,
     *                static initializer, field initializer, or
     *                explicit constructor invocation statement
     *                enclosing the statement or expression)
     * is <ul>
     *     <li>a static method</li>
     *     <li>a static initializer</li>
     *     <li>the variable initializer of a static variable</li>
     *     <li>an explicit constructor invocation statement</li>
     * </ul>
     */
    Stack<Boolean> inStaticContext;

    {
        inStaticContext = new Stack<>();
        inStaticContext.push(false);
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        outerClasses.pop();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        outerClasses.pop();
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        outerClasses.pop();
    }

    @Override
    public void endVisit(AnnotationTypeDeclaration node) {
        outerClasses.pop();
    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    public boolean getCurrentContext(BodyDeclaration bodyDeclaration) {
        List<IExtendedModifier> modifiers = bodyDeclaration.modifiers();
        return modifiers.stream()
                .anyMatch(m -> m instanceof Modifier modifier && modifier.isStatic());
    }

    private void noticeNewClass(ASTNode node) {
        typeDeclarations.add(node);
        if (! outerClasses.empty()) {
            ASTNode outerClass = outerClasses.peek();
            InnerClassManager.get().noticeInnerClass(node,
                    getBinding(outerClass),
                    inStaticContext.peek());
            outerClassMap.put(node, outerClass);
        }
        outerClasses.push(node);
    }

    @Override
    public void endVisit(Initializer node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(Initializer node) {
        inStaticContext.push(getCurrentContext(node));
        return true;
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        inStaticContext.push(getCurrentContext(node));
        return true;
    }

    @Override
    public void endVisit(FieldDeclaration node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        inStaticContext.push(getCurrentContext(node));
        return true;
    }

    @Override
    public void endVisit(ConstructorInvocation node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        inStaticContext.push(true);
        return true;
    }

    @Override
    public void endVisit(SuperConstructorInvocation node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        inStaticContext.push(true);
        return true;
    }

    @Override
    public void endVisit(EnumConstantDeclaration node) {
        inStaticContext.pop();
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        inStaticContext.push(true);
        return true;
    }

    public List<ASTNode> getTypeDeclarations() {
        return typeDeclarations;
    }

    public @Nullable String getOuterClass(ASTNode typeDeclaration) {
        ASTNode decl = outerClassMap.get(typeDeclaration);
        if (decl == null) {
            return null;
        }
        return JDTStringReps.getBinaryName(getBinding(decl));
    }

    public static ITypeBinding getBinding(ASTNode node) {
        if (node instanceof AbstractTypeDeclaration abstractTypeDeclaration) {
            return abstractTypeDeclaration.resolveBinding();
        } else if (node instanceof AnonymousClassDeclaration anonymousClassDeclaration) {
            return anonymousClassDeclaration.resolveBinding();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
