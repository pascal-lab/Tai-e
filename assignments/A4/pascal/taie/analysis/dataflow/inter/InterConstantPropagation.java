 /*
  * Tai-e: A Static Analysis Framework for Java
  *
  * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
  * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
  * All rights reserved.
  *
  * Tai-e is only for educational and academic purposes,
  * and any form of commercial use is disallowed.
  * Distribution of Tai-e is disallowed without the approval.
  */

 package pascal.taie.analysis.dataflow.inter;

 import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
 import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
 import pascal.taie.analysis.graph.cfg.CFG;
 import pascal.taie.analysis.graph.cfg.CFGBuilder;
 import pascal.taie.analysis.graph.icfg.CallEdge;
 import pascal.taie.analysis.graph.icfg.CallToReturnEdge;
 import pascal.taie.analysis.graph.icfg.NormalEdge;
 import pascal.taie.analysis.graph.icfg.ReturnEdge;
 import pascal.taie.config.AnalysisConfig;
 import pascal.taie.ir.IR;
 import pascal.taie.ir.exp.InvokeExp;
 import pascal.taie.ir.exp.Var;
 import pascal.taie.ir.stmt.Invoke;
 import pascal.taie.ir.stmt.Stmt;
 import pascal.taie.language.classes.JMethod;

 /**
  * Implementation of interprocedural constant propagation for int values.
  */
 public class InterConstantPropagation extends
         AbstractInterDataflowAnalysis<JMethod, Stmt, CPFact> {

     public static final String ID = "inter-constprop";

     private final ConstantPropagation cp;

     public InterConstantPropagation(AnalysisConfig config) {
         super(config);
         cp = new ConstantPropagation(new AnalysisConfig(ConstantPropagation.ID));
     }

     @Override
     public boolean isForward() {
         return cp.isForward();
     }

     @Override
     public CPFact newBoundaryFact(Stmt boundary) {
         IR ir = icfg.getContainingMethodOf(boundary).getIR();
         return cp.newBoundaryFact(ir.getResult(CFGBuilder.ID));
     }

     @Override
     public CPFact newInitialFact() {
         return cp.newInitialFact();
     }

     @Override
     public void meetInto(CPFact fact, CPFact target) {
         cp.meetInto(fact, target);
     }

     @Override
     protected boolean transferCallNode(Stmt stmt, CPFact in, CPFact out) {
         // TODO - finish me
         return false;
     }

     @Override
     protected boolean transferNonCallNode(Stmt stmt, CPFact in, CPFact out) {
         // TODO - finish me
         return false;
     }

     @Override
     protected CPFact transferNormalEdge(NormalEdge<Stmt> edge, CPFact out) {
         // TODO - finish me
         return null;
     }

     @Override
     protected CPFact transferCallToReturnEdge(CallToReturnEdge<Stmt> edge, CPFact out) {
         // TODO - finish me
         return null;
     }

     @Override
     protected CPFact transferCallEdge(CallEdge<Stmt> edge, CPFact callSiteOut) {
         // TODO - finish me
         return null;
     }

     @Override
     protected CPFact transferReturnEdge(ReturnEdge<Stmt> edge, CPFact returnOut) {
         // TODO - finish me
         return null;
     }
 }
