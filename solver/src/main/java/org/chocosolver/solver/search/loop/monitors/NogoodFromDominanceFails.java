package org.chocosolver.solver.search.loop.monitors;

import org.chocosolver.sat.MiniSat;
import org.chocosolver.sat.SatDecorator;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.constraints.nary.sat.PropSat;
import org.chocosolver.solver.constraints.nary.sat.NogoodStealer;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.DecisionPath;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.search.strategy.decision.SetDecision;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.objective.ObjectiveStrategy;
import org.chocosolver.solver.variables.*;
import org.chocosolver.util.tools.VariableUtils;

import java.util.ArrayDeque;
import java.util.Arrays;

public class NogoodFromDominanceFails implements IMonitorContradiction {

    @SuppressWarnings("rawtypes")
    private final ArrayDeque<Decision> decisions = new ArrayDeque<>(16);
    private final PropSat png;
    private final NogoodStealer nogoodStealer;

    private final java.util.Set<Class<? extends ICause>> causeClasses = new java.util.HashSet<>();
    private final java.util.Set<IntVar> objectiveVars =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private IntVar[] objectiveVarsArray;

    private boolean refutedCanBeParetoDominated;
    private boolean learnFromNonObjectiveFails;

    public NogoodFromDominanceFails(Model model, IntVar[] objectiveVars, boolean learnFromNonObjectiveFails) {
        this(model, objectiveVars, learnFromNonObjectiveFails, NogoodStealer.NONE, ParetoMaximizer.class);
    }

    @SafeVarargs
    public NogoodFromDominanceFails(Model model, IntVar[] objectiveVars, boolean learnFromNonObjectiveFails,
                                    NogoodStealer stealer, Class<? extends ICause>... causeKlasses) {
        this.png = model.getMinisat().getPropSat();
        this.nogoodStealer = stealer;
        this.nogoodStealer.add(model);
        this.refutedCanBeParetoDominated = false;
        this.learnFromNonObjectiveFails = learnFromNonObjectiveFails;

        this.objectiveVarsArray = objectiveVars;
        if (causeKlasses == null || causeKlasses.length == 0) {
            causeClasses.add(ParetoMaximizer.class);
        } else {
            for (Class<? extends ICause> k : causeKlasses) {
                if (k != null) causeClasses.add(k);
            }
            if (causeClasses.isEmpty()) {
                causeClasses.add(ParetoMaximizer.class);
            }
        }

        int bucketSize = getBucketSize(model);
        ((SatDecorator) this.png.getMiniSat()).enableDecisionBuckets(bucketSize);
    }

    private int getBucketSize(Model model) {
        if (model.getHook("decisionVariables") != null) {
            return ((IntVar[]) model.getHook("decisionVariables")).length;
        } else {
            return model.retrieveIntVars(true).length;
        }
    }

    public void addClassToFilter(Class<? extends ICause> causeClass) {
        if (causeClass != null) causeClasses.add(causeClass);
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        // --- 1) filter only dominance failures ---
        ICause cause = cex.c; // in Choco this is typically accessible; if not, use the proper getter
        if ((!learnFromNonObjectiveFails || ((SatDecorator) this.png.getMiniSat()).trimDynClausesAlready) &&
                !isDominanceFailure(cause, cex)) {
            return;
        }

        // --- 2) extract current path and build nogood ---
        DecisionPath dp = png.getModel().getSolver().getDecisionPath();
        if (refutedCanBeParetoDominated) {
            extractNogoodFromPath(dp);
        } else {
            extractNogoodFromPath_AllDecisions(dp);
        }

        // optional: share between equivalent models
//        nogoodStealer.nogoodStealing(png.getModel(), this);
    }

    private boolean isDominanceFailure(ICause cause, ContradictionException cex) {
        if (cause == null) return false;

        //return cause instanceof ParetoMaximizer;
        return cause instanceof ParetoMaximizer;

        // Generic version: accept subclasses too
//        for (Class<? extends ICause> k : causeClasses) {
//            if (k != null && k.isInstance(cause)){
//                if (objectiveVarsArray != null) {
//                    // todo verify that the cause tries to increase all the objectives values considering some cause objectives, maybe add a special interface for this?
//                } else {
//                    return false;
//                }
//            }
//        }
    }

    @SuppressWarnings("unchecked")
    public void extractNogoodFromPath(DecisionPath decisionPath) {
        assert decisions.isEmpty();
        decisionPath.transferInto(decisions, false);
        int d = decisions.size();
        int[] lits = new int[d];
        int i = 0;
        while (!decisions.isEmpty()) {
            Decision<Variable> decision = decisions.pollFirst();
            int lit = asLit(decision);
            if (decision.hasNext() || decision.getArity() == 1) {
                lits[i++] = lit;
            } else {
                if (i == 0) {
                    // value can be removed permanently from var!
                    png.addLearntUsingBuckets(0, lit);
//                    png.addLearnt(lit);
                } else {
                    lits[i] = lit;
//                    png.addLearnt(Arrays.copyOf(lits, i + 1));
                    png.addLearntUsingBuckets(i + 1, Arrays.copyOf(lits, i + 1));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void extractNogoodFromPath_AllDecisions(DecisionPath decisionPath) {
        assert decisions.isEmpty();
        decisionPath.transferInto(decisions, false);
        int d = decisions.size();
        if (d == 0) return;

        int[] lits = new int[d];
        int i = 0;

        while (!decisions.isEmpty()) {
            Decision<Variable> decision = decisions.pollFirst();
            lits[i++] = asLit(decision);
        }

        // Post exactly one learnt clause (or one unit).
        if (i == 1) {
//            png.addLearnt(lits[0]);
            png.addLearntUsingBuckets(0, lits[0]);
        } else {
//            png.addLearnt(Arrays.copyOf(lits, i));
            png.addLearntUsingBuckets(d, Arrays.copyOf(lits, i));
        }
    }

    /**
     * Transform this decision into a literal to be used in {@link PropSat}.
     *
     * @param decision a decision
     * @return the literal corresponding to this decision
     */
    private <V extends Variable> int asLit(Decision<V> decision) {
        if (decision instanceof IntDecision) {
            IntDecision id = (IntDecision) decision;
            return asLit(
                    nogoodStealer.getById(id.getDecisionVariable(), png.getModel()),
                    id.getDecOp(),
                    id.getDecisionValue()
            );
        } else if (decision instanceof SetDecision) {
            SetDecision id = (SetDecision) decision;
            return asLit(
                    nogoodStealer.getById(id.getDecisionVariable(), png.getModel()),
                    id.getDecOp(),
                    id.getDecisionValue()
            );
        } else {
            throw new UnsupportedOperationException("Cannot deal with such decision: " + decision);
        }
    }

    private int asLit(IntVar var, DecisionOperator<IntVar> op, int val) {
        if(VariableUtils.isBool(var)){
            return asLit((BoolVar) var, op, val);
        }
        int l;
        if (DecisionOperatorFactory.makeIntEq().equals(op)) {
            l = MiniSat.makeLiteral(png.makeIntEq(var, val), false);
        } else if (DecisionOperatorFactory.makeIntNeq().equals(op)) {
            l = MiniSat.makeLiteral(png.makeIntEq(var, val), true);
        } else if (DecisionOperatorFactory.makeIntSplit().equals(op)
                || op instanceof ObjectiveStrategy.BottomUpDecisionOperator) {
            l = MiniSat.makeLiteral(png.makeIntLe(var, val), false);
        } else if (DecisionOperatorFactory.makeIntReverseSplit().equals(op)
                || op instanceof ObjectiveStrategy.TopDownDecisionOperator) {
            l = MiniSat.makeLiteral(png.makeIntLe(var, val), true);
        } else {
            throw new UnsupportedOperationException("Cannot deal with such operator: " + op);
        }
        return l;
    }

    private int asLit(BoolVar var, DecisionOperator<IntVar> op, int val) {
        int l;
        if (DecisionOperatorFactory.makeIntEq().equals(op)) {
            assert val == 0 || val == 1 : "Value must be either 0 or 1 for BoolVar";
            l = MiniSat.makeLiteral(png.makeBool(var), val == 0);
        } else if (DecisionOperatorFactory.makeIntNeq().equals(op)) {
            assert val == 0 || val == 1 : "Value must be either 0 or 1 for BoolVar";
            l = MiniSat.makeLiteral(png.makeBool(var), val == 1);
        } else if (DecisionOperatorFactory.makeIntSplit().equals(op)
                || op instanceof ObjectiveStrategy.BottomUpDecisionOperator) {
            assert val == 0  : "Value must be 0";
            l = MiniSat.makeLiteral(png.makeBool(var), true);
        } else if (DecisionOperatorFactory.makeIntReverseSplit().equals(op)
                || op instanceof ObjectiveStrategy.TopDownDecisionOperator) {
            assert val == 1  : "Value must be 1";
            l = MiniSat.makeLiteral(png.makeBool(var), false);
        } else {
            throw new UnsupportedOperationException("Cannot deal with such operator: " + op);
        }
        return l;
    }

    private int asLit(SetVar var, DecisionOperator<SetVar> op, int val) {
        int l;
        if (DecisionOperatorFactory.makeSetForce().equals(op)) {
            l = MiniSat.makeLiteral(png.makeSetIn(var, val), false);
        } else if (DecisionOperatorFactory.makeSetRemove().equals(op)) {
            l = MiniSat.makeLiteral(png.makeSetIn(var, val), true);
        } else {
            throw new UnsupportedOperationException("Cannot deal with such operator: " + op);
        }
        return l;
    }

    public void setRefutedCanBeParetoDominated(boolean refutedCanBeParetoDominated) {
        this.refutedCanBeParetoDominated = refutedCanBeParetoDominated;
    }

    public void setLearnFromNonObjectiveFails (boolean learnFromNonObjectiveFails) {
        this.learnFromNonObjectiveFails = learnFromNonObjectiveFails;
    }
}
