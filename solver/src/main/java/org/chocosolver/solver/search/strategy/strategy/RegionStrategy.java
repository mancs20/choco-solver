package org.chocosolver.solver.search.strategy.strategy;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.objective.OptimizationPolicy;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.variables.IntVar;

import static org.chocosolver.solver.objective.OptimizationPolicy.DICHOTOMIC;

public class RegionStrategy extends AbstractStrategy<IntVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int globalLB, globalUB;
//    private final int definedLB;
//    private final int definedUB;
    private int bound;
    private final boolean isLowerBound;
    private IntVar obj;

    private IntVar[] objs;
    int idxObjsDecision = 0;

    private final Model model;
    private boolean firstCall;
    private DecisionOperator<IntVar> decOperator;
//    private DecisionOperator<IntVar> boundOperator;
//    private final OptimizationPolicy optPolicy;
//    public final DecisionOperator<IntVar> decUB = new BottomUpDecisionOperator();
//    public final DecisionOperator<IntVar> incLB = new TopDownDecisionOperator();
    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     *
//     * @param objectives variable
     *
     */
    public RegionStrategy(IntVar[] objectives, int[] lowerBounds) {
        super(objectives);
        this.objs = objectives;
        this.model = objs[0].getModel();
        this.firstCall = true;
//        this.bound = bound;
        this.isLowerBound = true;
    }



    public RegionStrategy(IntVar objective, int bound, boolean isLowerBound) {
        super(new IntVar[]{objective});
        this.obj = objective;
        this.model = obj.getModel();
        this.firstCall = true;
        this.bound = bound;
        this.isLowerBound = isLowerBound;

//        model.getSolver().setRestartOnSolutions();
//        if (coefLB < 0 || coefUB < 0 || coefLB + coefUB == 0) {
//            throw new UnsupportedOperationException("coefLB<0, coefUB<0 and coefLB+coefUB==0 are forbidden");
//        }
//        if (coefLB + coefUB != 1 && policy != DICHOTOMIC) {
//            throw new UnsupportedOperationException("Invalid coefficients for BOTTOM_UP or TOP_DOWN optimization" +
//                    "\nuse signature public RegionStrategy(IntVar obj, OptimizationPolicy policy, Model model) instead");
//        }
    }

//    private static int[] getCoefs(OptimizationPolicy policy) {
//        switch (policy) {
//            case BOTTOM_UP:
//                return new int[]{1, 0};
//            case TOP_DOWN:
//                return new int[]{0, 1};
//            case DICHOTOMIC:
//                return new int[]{1, 1};
//            default:
//                throw new UnsupportedOperationException("unknown OptimizationPolicy " + policy);
//        }
//    }

//    private DecisionOperator<IntVar> getOperator(OptimizationPolicy optPolicy, ResolutionPolicy resoPolicy) {
//        switch (optPolicy) {
//            case BOTTOM_UP:
//                return decUB;
//            case TOP_DOWN:
//                return incLB;
//            case DICHOTOMIC:
//                switch (resoPolicy) {
//                    case MINIMIZE:
//                        return decUB;
//                    case MAXIMIZE:
//                        return incLB;
//                    default:
//                        throw new UnsupportedOperationException("RegionStrategy is not for " + resoPolicy + " ResolutionPolicy");
//                }
//            default:
//                throw new UnsupportedOperationException("unknown OptimizationPolicy " + optPolicy);
//        }
//    }
     private DecisionOperator<IntVar> getOperator() {
        return isLowerBound ? DecisionOperatorFactory.makeIntReverseSplit() : DecisionOperatorFactory.makeIntSplit();
//        return isLowerBound ? DecisionOperatorFactory.makeIntReverseSplit() : DecisionOperatorFactory.makeIntSplit();
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    public boolean init() {
        decOperator = getOperator();
//        decOperator = getOperator(optPolicy, model.getSolver().getObjectiveManager().getPolicy());
//        boundOperator = DecisionOperatorFactory.makeIntSplit();
//        boundOperator = DecisionOperatorFactory.makeIntReverseSplit();
        return true;
    }

    @Override
    public void remove() {

    }

//    @Override
//    public Decision<IntVar> getDecision() {
//        if (obj.isInstantiated()) {
//            return null;
//        }
//
//        if ((isLowerBound && obj.getLB() >= bound) || (!isLowerBound && obj.getUB() <= bound)) {
//            return null;
//        }
//
//
////        globalLB = Math.max(globalLB, obj.getLB());//check
////        globalUB = Math.min(globalUB, obj.getUB());//check
//        //        ObjectiveManager man = model.getResolver().getObjectiveManager();
//        //        man.updateLB(globalLB);
//        //        man.updateUB(globalUB);
//        if (obj.getLB() > obj.getUB()) {
//            return null;
//        }
//
////        if (globalLB >= definedLB){
////            return null;
////        }
//
//        if (firstCall) {
//            firstCall = false;
//        }
//
//        String operator = isLowerBound ? ">=" : "<=";
//        if (model.getSettings().warnUser()) {
//            model.getSolver().log().bold().println("objective " + obj + " " + operator + " " + bound);
//        }
//
////        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, target);
//        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, bound);
//        if (model.getSettings().warnUser()) {
//            model.getSolver().log().bold().println("- trying " + obj + " " + operator + " " + bound);
//        }
//        dec.setRefutable(false);
//        return dec;
//    }

    @Override
    public Decision<IntVar> getDecision() {
        if (idxObjsDecision >= objs.length) {
            idxObjsDecision = 0;
            return null;
        }
        return computeDecision(objs[idxObjsDecision]);
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar obj) {
        if (obj.isInstantiated()) {
            idxObjsDecision++;
            return null;
        }

        if ((isLowerBound && obj.getLB() >= bound) || (!isLowerBound && obj.getUB() <= bound)) {
            return null;
        }


//        globalLB = Math.max(globalLB, obj.getLB());//check
//        globalUB = Math.min(globalUB, obj.getUB());//check
        //        ObjectiveManager man = model.getResolver().getObjectiveManager();
        //        man.updateLB(globalLB);
        //        man.updateUB(globalUB);
        if (obj.getLB() > obj.getUB()) {
            return null;
        }

//        if (globalLB >= definedLB){
//            return null;
//        }

        if (firstCall) {
            firstCall = false;
        }

        String operator = isLowerBound ? ">=" : "<=";
        if (model.getSettings().warnUser()) {
            model.getSolver().log().bold().println("objective " + obj + " " + operator + " " + bound);
        }

//        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, target);
        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, bound);
        if (model.getSettings().warnUser()) {
            model.getSolver().log().bold().println("- trying " + obj + " " + operator + " " + bound);
        }
        dec.setRefutable(false);
        return dec;
    }

//    public class ParetoDecisionOperator implements DecisionOperator<IntVar> {
//        //FIXME can not serialize decision
//
//        @Override
//        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
//            return var.updateUpperBound(value, cause);
//        }
//
//        @Override
//        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
//            globalLB = value + 1;
//            //            model.getResolver().getObjectiveManager().updateLB(globalLB);
//            return var.updateLowerBound(globalLB, cause);
//        }
//
//        @Override
//        public String toString() {
//            return " objective split(" + coefLB + "," + coefUB + "), decreases the upper bound first";
//        }
//
//        @Override
//        public DecisionOperator<IntVar> opposite() {
//            return incLB;
//        }
//    }
//
//    public class TopDownDecisionOperator implements DecisionOperator<IntVar> {
//        //FIXME can not serialize decision
//        @Override
//        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
//            return var.updateLowerBound(value, cause);
//        }
//
//        @Override
//        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
//            globalUB = value - 1;
//            //            model.getResolver().getObjectiveManager().updateUB(globalUB);
//            return var.updateUpperBound(globalUB, cause);
//        }
//
//        @Override
//        public String toString() {
//            return " objective split(" + coefLB + "," + coefUB + "), increases the lower bound first";
//        }
//
//        @Override
//        public DecisionOperator<IntVar> opposite() {
//            return decUB;
//        }
//    }
}
