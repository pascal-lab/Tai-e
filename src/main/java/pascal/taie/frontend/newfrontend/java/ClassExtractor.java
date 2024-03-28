package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.IMember;
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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
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

    @Override
    public boolean visit(LambdaExpression node) {
        // lambda is a special kind of method
        // it does not affect the static context,
        // because it's up to compiler to decide whether to generate a static method
        IMethodBinding binding = node.resolveMethodBinding();
        boolean isStaticContext = inStaticContext.peek();
        IMethodBinding functionalInterface = binding.getMethodDeclaration();
        LambdaManager.get().addLambda(node, binding, functionalInterface,
                getCurrentClass(), isStaticContext);
        outerClasses.add(node);
        return true;
    }

    @Override
    public void endVisit(LambdaExpression node) {
        ASTNode outNode = outerClasses.pop();
        // check for misuse, if occurs, it must be a bug
        assert outNode == node;
        Set<IVariableBinding> capturedVars = computeDirectInnerSet(node);
        for (IVariableBinding captureVariable : capturedVars) {
            LambdaManager.get().getDescriptor(node.resolveMethodBinding())
                    .addCaptured(captureVariable);
        }
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
            ITypeBinding outer;
            if (outerClass instanceof LambdaExpression le) {
                outer = getLambdaDeclaredIn(le);
            } else {
                outer = getBinding(outerClass);
            }
            InnerClassManager.get().noticeInnerClass(node,
                    outer,
                    inStaticContext.peek());
            outerClassMap.put(node, outerClass);
        }
        outerClasses.push(node);
    }

    private Set<IVariableBinding> computeDirectInnerSet(ASTNode node) {
        // find directly inner classes
        Set<IVariableBinding> res = Sets.newSet();
        List<ASTNode> innerClasses = new ArrayList<>();
        for (ASTNode typeDeclaration : typeDeclarations) {
            if (outerClassMap.get(typeDeclaration) == node) {
                innerClasses.add(typeDeclaration);
            }
        }
        // resolve capture variables
        for (ASTNode innerClass : innerClasses) {
            for (IVariableBinding captureVariable : getCapturedVarSet(innerClass)) {
                // when reach here, captureVariable must be a local variable or a parameter
                // i.e. they must belong to a method (m).
                // if (m) is a method of class (node),
                // then this variable should not be captured by (node).
                if (!isDeclaredIn(node, captureVariable)) {
                    res.add(captureVariable);
                }
            }
        }
        return res;
    }

    private Set<IVariableBinding> computeDirectCapturedSet(Set<IVariableBinding> capturedSet, ASTNode directOuter) {
        // if outer is lambda, we have to capture all the variables
        if (directOuter instanceof LambdaExpression) {
            return capturedSet;
        }
        Set<IVariableBinding> res = Sets.newSet();
        ITypeBinding current = getBinding(directOuter);
        for (IVariableBinding v : capturedSet) {
            assert !v.isField();
            // check for misuse, inner class can only capture variables from outer classes
            // assert isTransitiveOuter(current, v.getDeclaringMethod().getDeclaringClass());
            if (v.getDeclaringMethod().getDeclaringClass().equals(current)) {
                res.add(v);
            }
        }
        return res;
    }

    private Set<IVariableBinding> getCapturedVarSet(ASTNode node) {
        if (node instanceof LambdaExpression le) {
            return LambdaManager.get().getDescriptor(le.resolveMethodBinding()).allCaptured();
        } else {
            InnerClassDescriptor descriptor =
                    InnerClassManager.get().getInnerClassDesc(getBinding(node));
            return descriptor.capturedVars();
        }
    }

    private boolean isDeclaredIn(ASTNode node, IVariableBinding v) {
        IBinding binding = getBindingAll(node);
        if (binding instanceof IMethodBinding m) {
            return m.equals(v.getDeclaringMethod());
        } else {
            return v.getDeclaringMethod().getDeclaringClass().equals(binding);
        }
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
        ITypeBinding current = getBinding(node);
        InnerClassDescriptor currentDescriptor =
                InnerClassManager.get().getInnerClassDesc(current);
        if (currentDescriptor == null) {
            return;
        }
        Set<IVariableBinding> innerCaptured = computeDirectInnerSet(node);
        for (IVariableBinding captureVariable : innerCaptured) {
            currentDescriptor.addNewCapture(captureVariable);
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
        Set<IVariableBinding> directedCaptured =
                computeDirectCapturedSet(currentDescriptor.capturedVars(), directOuter);
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
        // ignore the simple name in the top-level of lambda expression
        // captures of lambda is recorded by JDT
        // we just make sure it does not affect the inner class capture
        if (outerClasses.peek() instanceof LambdaExpression) {
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
        if (InnerClassManager.isLocal(t) && !isDefineInCurrentMethod(t)) {
            // apply rule:
            //  if a local class is initialized,
            //  then its captured vars must be captured by its outer class
            ASTNode current = outerClasses.peek();
            Set<IVariableBinding> capturedVars =
                    InnerClassManager.get().getInnerClassDesc(t).capturedVars();
            unionCapture(getBinding(current), capturedVars);
        }
        // there may be anonymous class in the tail of the constructor invocation
        return true;
    }

    private boolean isDefineInCurrentMethod(ITypeBinding binding) {
        ASTNode decl = outerClasses.peek();
        if (decl instanceof LambdaExpression le) {
            return true;
        } else {
            return binding.getDeclaringMethod().getDeclaringClass().equals(getBinding(decl));
        }
    }

    public List<ASTNode> getTypeDeclarations() {
        return typeDeclarations;
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

    private static IBinding getBindingAll(ASTNode node) {
       if (node instanceof LambdaExpression le) {
           return le.resolveMethodBinding();
       } else {
           return getBinding(node);
       }
    }

    private void unionCapture(IBinding binding, Set<IVariableBinding> capturedVars) {
        for (IVariableBinding v : capturedVars) {
            if (binding instanceof IMethodBinding m) {
                LambdaManager.get().getDescriptor(m).addCaptured(v);
            } else {
                InnerClassManager.get().noticeCaptureVariableBinding(v, (ITypeBinding) binding);
            }
        }
    }

    public static ITypeBinding getLambdaDeclaredIn(LambdaExpression le) {
        IBinding outer = le.resolveMethodBinding().getDeclaringMember();
        if (outer instanceof ITypeBinding t) {
            return t;
        } else if (outer instanceof IMethodBinding m) {
            return m.getDeclaringClass();
        } else if (outer instanceof IVariableBinding v) {
            if (v.getDeclaringMethod() == null) {
                return v.getDeclaringClass();
            } else {
                return v.getDeclaringMethod().getDeclaringClass();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
