package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;


public class SaugmeconUpdateModifyUB extends AbstractSaugmeconUpdate {

    private Constraint mainObjectiveUB;
    private int mainObjectiveLastValue;

    @Override
    protected void onStartUpdate(IntVar[] objectives, Solution solution) {
        mainObjectiveLastValue = Integer.MAX_VALUE;

        if (mainObjectiveUB != null) {
            objectives[0].getModel().unpost(mainObjectiveUB);
            mainObjectiveUB = null;
        }
    }

    @Override
    protected void onAfterUpdateRegionConstraints(IntVar objective) {
        // post the UB right before break (your current logic)
        if (mainObjectiveLastValue != Integer.MAX_VALUE) {
            mainObjectiveUB = objective.getModel().arithm(objective, "<=", mainObjectiveLastValue);
            mainObjectiveUB.post();
        }
    }

    @Override
    protected void onEndProcessingSolution() {
        mainObjectiveLastValue = Integer.MAX_VALUE;
    }

    @Override
    protected void onPreviousSolutionNotDominating(int[] fSolutionValues) {
        if (mainObjectiveLastValue > fSolutionValues[0]) {
            mainObjectiveLastValue = fSolutionValues[0];
        }
    }
}