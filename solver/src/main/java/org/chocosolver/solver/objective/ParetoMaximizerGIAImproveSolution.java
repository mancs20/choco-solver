package org.chocosolver.solver.objective;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class ParetoMaximizerGIAImproveSolution extends Propagator<IntVar> implements IMonitorSolution {
    protected int[] lastObjectiveVal;
    private final IntVar[] objectives;
    private final int n;
    private boolean activated;
    private final boolean reduceUB;
    private int[] highestCurrentUpperBounds;

    public ParetoMaximizerGIAImproveSolution(IntVar[] objectives, boolean reactToFineEvt, boolean reduceUB) {
        // Lowest priority to ensure that the propagator is called before the others
        super(objectives, PropagatorPriority.UNARY, reactToFineEvt);
        this.objectives = objectives;
        this.n = objectives.length;
        lastObjectiveVal = new int[n];
        activated = false;
        this.reduceUB = reduceUB;
        highestCurrentUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            highestCurrentUpperBounds[i] = objectives[i].getUB();
        }
    }

//    @Override
//    public int getPropagationConditions(int vIdx) {
//        return IntEventType.upperBoundAndInst();
//    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (activated) {
            computeDominatedArea();
        }
    }

    protected void computeDominatedArea() throws ContradictionException{
        boolean someLBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]){
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }else if (objectives[i].getLB() > lastObjectiveVal[i]){
                someLBBiggerThanLastSolution = true;
            }
        }

        if (!someLBBiggerThanLastSolution){
            //all the lower bounds are equal to the last solution, there should be at least one upper bound that is bigger
            //if not then fails
            boolean atLeastOneUBBiggerThanLastSolution = false;
            int idOfTheUBBiggerThanLastSolution = -1; // -1 means that there is more than one upper bound bigger than the last solution
            // check that current solution is bigger than the last one
            for (int i = 0; i < n; i++) {
                if (objectives[i].getUB() > lastObjectiveVal[i]){
                    if (atLeastOneUBBiggerThanLastSolution){
                        idOfTheUBBiggerThanLastSolution = -1;
                        break;
                    }else{
                        atLeastOneUBBiggerThanLastSolution = true;
                        idOfTheUBBiggerThanLastSolution = i;
                    }
                }
            }
            if (atLeastOneUBBiggerThanLastSolution){
                if (idOfTheUBBiggerThanLastSolution != -1) {
                    // only one objective has bigger upper bound than the last solution
                    objectives[idOfTheUBBiggerThanLastSolution].updateLowerBound(lastObjectiveVal[idOfTheUBBiggerThanLastSolution] + 1, this);
                }
            }else{
                fails();
            }
        } //else{
        // all lower bounds are equal or greater than the last solution with at least one lower bound greater than
        // the last solution. setPassive is not working
//            this.setPassive();
//        }
    }

    @Override
    public void onSolution() {
        for (int i = 0; i < n; i++) {
            lastObjectiveVal[i] = objectives[i].getValue();
        }
    }

//    public void setActivated(boolean activated, int[] highestCurrentUpperBounds) {
//        this.activated = activated;
//        this.highestCurrentUpperBounds = highestCurrentUpperBounds;
//    }

    public void setDeactivated() {
        this.activated = false;
    }

    public void setActivated(int[] lastObjectiveVal) {
        this.activated = true;
        this.lastObjectiveVal = lastObjectiveVal;
    }

    public void setHighestCurrentUpperBounds(int[] highestCurrentUpperBounds) {
        this.highestCurrentUpperBounds = highestCurrentUpperBounds;
    }

    @Override
    public ESat isEntailed() {
        return ESat.TRUE;
    }
}
