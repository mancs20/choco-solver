package org.chocosolver.solver.objective;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class ParetoTestK5050W06 extends Propagator<IntVar> implements IMonitorSolution {

    private final IntVar[] objectives;
    private final int n;

    // varaibles for one point
    private int[] lowerBounds;
    private int[] upperBounds;


    public ParetoTestK5050W06(IntVar[] objectives) {
        super(objectives, PropagatorPriority.QUADRATIC, false);
        this.objectives = objectives;
        this.n = objectives.length;
        lowerBounds = new int[n];
        upperBounds = new int[n];

        for (int i = 0; i < n; i++) {
            lowerBounds[i] = objectives[i].getLB();
            upperBounds[i] = objectives[i].getUB();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lowerBounds[i]){
                objectives[i].updateLowerBound(lowerBounds[i]+1, this);
            }
            if (objectives[i].getUB() > upperBounds[i]){
                objectives[i].updateUpperBound(upperBounds[i] -1, this);
            }
        }
    }

    @Override
    public ESat isEntailed() {
        return ESat.TRUE;
    }

    @Override
    public void onSolution() {
        for (int i = 0; i < n; i++) {
            lowerBounds[i] = objectives[i].getValue();
            upperBounds[i] = objectives[i].getValue();
        }
    }

    public void updateRegions(int[] lowerBounds, int[] upperBounds) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;

//        for (int i = 0; i < objectives.length; i++) {
//            this.model.getSolver().getDecisionPath().makeIntDecision(objectives[i], DecisionOperatorFactory.makeIntSplit(), upperBounds[i]);
//            this.model.getSolver().getDecisionPath().makeIntDecision(objectives[i], DecisionOperatorFactory.makeIntReverseSplit(), lowerBounds[i]);
//        }


    }

}
