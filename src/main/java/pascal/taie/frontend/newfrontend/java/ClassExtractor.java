package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        resolveCapture(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        resolveCapture(node);
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        noticeNewClass(node);
        return true;
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        resolveCapture(node);
    }

    @Override
    public void endVisit(AnnotationTypeDeclaration node) {
        resolveCapture(node);
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
        @SuppressWarnings("unchecked")
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

    /**
     * resolving capture variables when exiting an inner class
     * @param node the inner class
     */
    private void resolveCapture(ASTNode node) {
        ASTNode outNode = outerClasses.pop();
        // check for misuse, if occurs, it must be a bug
        assert outNode == node;
        // if the current class is not an inner class,
        // then there is no need to resolve capture
        if (outerClasses.empty()) {
            return;
        }
        ASTNode directOuter = outerClasses.peek();
        // find directly inner classes
        List<ASTNode> innerClasses = new ArrayList<>();
        for (ASTNode typeDeclaration : typeDeclarations) {
            if (outerClassMap.get(typeDeclaration) == node) {
                innerClasses.add(typeDeclaration);
            }
        }
        // resolve capture variables
        ITypeBinding current = getBinding(node);
        InnerClassDescriptor currentDescriptor =
                InnerClassManager.get().getInnerClassDesc(current);
        if (currentDescriptor == null) {
            return;
        }
        for (ASTNode innerClass : innerClasses) {
            InnerClassDescriptor descriptor =
                    InnerClassManager.get().getInnerClassDesc(getBinding(innerClass));
            for (IVariableBinding captureVariable : descriptor.capturedVars()) {
                // when reach here, captureVariable must be a local variable or a parameter
                // i.e. they must belong to a method (m).
                // if (m) is a method of class (node),
                // then this variable should not be captured by (node).
                if (captureVariable.getDeclaringMethod().getDeclaringClass() != getBinding(node)) {
                    currentDescriptor.addNewCapture(captureVariable);
                }
            }
        }
        // resolving for inherited inner classes, this is a little tricky
        ITypeBinding parent = current.getSuperclass();
        while (parent != null && parent.isLocal() && !isOneOfOuter(parent)) {
            InnerClassDescriptor parentDescriptor =
                    InnerClassManager.get().getInnerClassDesc(parent);
            for (IVariableBinding captureVariable : parentDescriptor.capturedVars()) {
                // need not check if the variable is captured by (node),
                // because a class cannot inherit from itself or its inner class
                currentDescriptor.addNewCapture(captureVariable);
            }
            parent = parent.getSuperclass();
        }
        // finally, the captured set is resolved, we then resolve the direct captured set
        // and set the synthetic parameters
        Set<IVariableBinding> directedCaptured = Sets.newSet();
        ITypeBinding directOuterClass = getBinding(directOuter);
        for (IVariableBinding v : currentDescriptor.capturedVars()) {
            assert !v.isField();
            // check for misuse, inner class can only capture variables from outer classes
            assert isTransitiveOuter(current, v.getDeclaringMethod().getDeclaringClass());
            if (v.getDeclaringMethod().getDeclaringClass().equals(directOuterClass)) {
                directedCaptured.add(v);
            }
        }
        currentDescriptor.resolveSynPara(directedCaptured);
    }

    private boolean isOneOfOuter(ITypeBinding v) {
        return outerClasses.stream()
                .map(ClassExtractor::getBinding)
                .anyMatch(b -> b.equals(v));
    }

    /**
     * Check if the inner class is transitive outer of the outer class
     * @param inner the inner class
     * @param outer the outer class
     * @return true if the inner class is transitive outer of the outer class
     */
    private boolean isTransitiveOuter(ITypeBinding inner, ITypeBinding outer) {
        ITypeBinding parent = inner.getDeclaringClass();
        while (parent != null) {
            if (parent.equals(outer)) {
                return true;
            }
            parent = parent.getDeclaringClass();
        }
        return false;
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

    @Override
    public boolean visit(SimpleName sn) {
        if (outerClasses.empty()) {
            return false;
        }
        IBinding b = sn.resolveBinding();
        ITypeBinding currentClass = getCurrentClass();
        if (b instanceof IVariableBinding v) {
            // is a captured variable
            if (!v.isField() && v.getDeclaringMethod().getDeclaringClass() != currentClass) {
                InnerClassManager.get().noticeCaptureVariableBinding(v, currentClass);
            }
        }
        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation newStmt) {
        if (outerClasses.empty()) {
            return true;
        }
        ITypeBinding t = newStmt.resolveConstructorBinding().getDeclaringClass();
        if (InnerClassManager.isLocal(t) && t.getDeclaringClass() != getCurrentClass()) {
            InnerClassManager.get().noticeLocalClassInit(t, getCurrentClass());
        }
        // there may be anonymous class in the tail of the constructor invocation
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

    private ITypeBinding getCurrentClass() {
        return getBinding(outerClasses.peek());
    }
}
