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
    private final int coefLB;
    private final int coefUB;
    private final int definedLB;
    private final int definedUB;
    private final IntVar obj;
    private long nbSols;
    private final Model model;
    private boolean firstCall;
    private DecisionOperator<IntVar> decOperator;
    private DecisionOperator<IntVar> boundOperator;
    private final OptimizationPolicy optPolicy;
    public final DecisionOperator<IntVar> decUB = new BottomUpDecisionOperator();
    public final DecisionOperator<IntVar> incLB = new TopDownDecisionOperator();
    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Defines a branching strategy over the objective variable
     * BEWARE: only activated after a first solution
     *
     * @param objective variable
     * @param policy    BOTTOM_UP, TOP_TOWN or DICHOTOMIC
     */
//    public RegionStrategy(IntVar objective, OptimizationPolicy policy) {
//        this(objective, getCoefs(policy), policy);
//    }

    /**
     * Defines a parametrized dichotomic branching over the objective variable
     * BEWARE: only activated after a first solution
     *
     * @param objective variable
     * @param coefs     [a,b] defines how to split the domain of the objective variable
     *                  [1,1] will halve its domain
     *                  [1,2] will take a value closer to the upper bound than the lower bound
     * @param policy    should be DICHOTOMIC
     */
    public RegionStrategy(IntVar objective, int[] coefs, int[] bounds, OptimizationPolicy policy) {
        super(new IntVar[]{objective});
        this.obj = objective;
        this.model = obj.getModel();
        this.firstCall = true;
        this.coefLB = coefs[0];
        this.coefUB = coefs[1];
        this.definedLB = bounds[0];
        this.definedUB = bounds[1];
        this.optPolicy = policy;
        model.getSolver().setRestartOnSolutions();
        if (coefLB < 0 || coefUB < 0 || coefLB + coefUB == 0) {
            throw new UnsupportedOperationException("coefLB<0, coefUB<0 and coefLB+coefUB==0 are forbidden");
        }
        if (coefLB + coefUB != 1 && policy != DICHOTOMIC) {
            throw new UnsupportedOperationException("Invalid coefficients for BOTTOM_UP or TOP_DOWN optimization" +
                    "\nuse signature public RegionStrategy(IntVar obj, OptimizationPolicy policy, Model model) instead");
        }
    }

    private static int[] getCoefs(OptimizationPolicy policy) {
        switch (policy) {
            case BOTTOM_UP:
                return new int[]{1, 0};
            case TOP_DOWN:
                return new int[]{0, 1};
            case DICHOTOMIC:
                return new int[]{1, 1};
            default:
                throw new UnsupportedOperationException("unknown OptimizationPolicy " + policy);
        }
    }

    private DecisionOperator<IntVar> getOperator(OptimizationPolicy optPolicy, ResolutionPolicy resoPolicy) {
        switch (optPolicy) {
            case BOTTOM_UP:
                return decUB;
            case TOP_DOWN:
                return incLB;
            case DICHOTOMIC:
                switch (resoPolicy) {
                    case MINIMIZE:
                        return decUB;
                    case MAXIMIZE:
                        return incLB;
                    default:
                        throw new UnsupportedOperationException("RegionStrategy is not for " + resoPolicy + " ResolutionPolicy");
                }
            default:
                throw new UnsupportedOperationException("unknown OptimizationPolicy " + optPolicy);
        }
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    public boolean init() {
        decOperator = getOperator(optPolicy, model.getSolver().getObjectiveManager().getPolicy());
        boundOperator = DecisionOperatorFactory.makeIntSplit();
        boundOperator = DecisionOperatorFactory.makeIntReverseSplit();
        return true;
    }

    @Override
    public void remove() {

    }

    @Override
    public Decision<IntVar> getDecision() {
//        if (model.getSolver().getSolutionCount() == 0
//                || (nbSols == model.getSolver().getSolutionCount() && optPolicy == DICHOTOMIC)) {
//            return null;
//        }
        if (obj.isInstantiated()) {
            return null;
        }
        if (firstCall) {
            firstCall = false;
            globalLB = obj.getLB();
            globalUB = obj.getUB();
        }
        nbSols = model.getSolver().getSolutionCount();
        globalLB = Math.max(globalLB, obj.getLB());//check
        globalUB = Math.min(globalUB, obj.getUB());//check
        //        ObjectiveManager man = model.getResolver().getObjectiveManager();
        //        man.updateLB(globalLB);
        //        man.updateUB(globalUB);
        if (globalLB > globalUB) {
            return null;
        }

        if (globalLB >= definedLB){
            return null;
        }

        if (model.getSettings().warnUser()) {
            model.getSolver().log().bold().println("- objective in [" + globalLB + ", " + globalUB + "]");
        }
        int target;
        target = (globalLB * coefLB + globalUB * coefUB) / (coefLB + coefUB);
//        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, decOperator, target);
        IntDecision dec = model.getSolver().getDecisionPath().makeIntDecision(obj, boundOperator, definedLB);
        if (model.getSettings().warnUser()) {
//            model.getSolver().log().bold().println("- trying " + obj + " " + (decOperator == decUB ? "<=" : ">=") + " " + target);
        }
        dec.setRefutable(false);
        return dec;
    }

    public class BottomUpDecisionOperator implements DecisionOperator<IntVar> {
        //FIXME can not serialize decision

        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateUpperBound(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            globalLB = value + 1;
            //            model.getResolver().getObjectiveManager().updateLB(globalLB);
            return var.updateLowerBound(globalLB, cause);
        }

        @Override
        public String toString() {
            return " objective split(" + coefLB + "," + coefUB + "), decreases the upper bound first";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return incLB;
        }
    }

    public class TopDownDecisionOperator implements DecisionOperator<IntVar> {
        //FIXME can not serialize decision
        @Override
        public boolean apply(IntVar var, int value, ICause cause) throws ContradictionException {
            return var.updateLowerBound(value, cause);
        }

        @Override
        public boolean unapply(IntVar var, int value, ICause cause) throws ContradictionException {
            globalUB = value - 1;
            //            model.getResolver().getObjectiveManager().updateUB(globalUB);
            return var.updateUpperBound(globalUB, cause);
        }

        @Override
        public String toString() {
            return " objective split(" + coefLB + "," + coefUB + "), increases the lower bound first";
        }

        @Override
        public DecisionOperator<IntVar> opposite() {
            return decUB;
        }
    }
}
