package org.chocosolver.solver.objective;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;

public class MaxIntObjManagerWithObjsLB extends MaxIntObjManager {

    private final IntVar[] objectives;
    private int[] lastLB;
    private boolean applyObjectivesLB = false;
    private static final long serialVersionUID = 1L;

    public MaxIntObjManagerWithObjsLB(IntVar objectiveFunction, IntVar[] objectives) {
        super(objectiveFunction);
        this.objectives = objectives;
    }

    public void setObjectivesLowerBound(int[] lastLB) {
        this.lastLB = lastLB;
        applyObjectivesLB = true;
    }

    @Override
    public void postDynamicCut() throws ContradictionException {
        super.postDynamicCut();
        if (applyObjectivesLB) {
            for (int i = 0; i < objectives.length; i++) {
                objectives[i].updateLowerBound(lastLB[i], this);
            }
        }
    }

    @Override
    public void resetBestBounds() {
        super.resetBestBounds();
        applyObjectivesLB = false;
    }
}
