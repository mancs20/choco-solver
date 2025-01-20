package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Priority;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.List;

public abstract class ParetoMaximizerGIAGeneral extends Propagator<IntVar> implements IMonitorSolution {
    protected final IntVar[] objectives;
    protected final int n;
    protected final Model model;

    protected GiaConfig.BoundedType boundedType;

    protected final boolean portfolio;
    protected boolean findingFirstPoint;
    protected final List<int[]> paretoFront;

    protected Solution lastSolution;
    protected int[] lastObjectiveVal;
    protected int[] lowestUpperBounds;
    // --------------------------------------------------------


    public ParetoMaximizerGIAGeneral(IntVar[] objectives, Priority priority, boolean reactToFineEvt,
                                     boolean portfolio) {
        super(objectives, priority, reactToFineEvt);
        this.objectives = objectives.clone();
        n = objectives.length;
        model = this.objectives[0].getModel();
        this.portfolio = portfolio;
        this.boundedType = GiaConfig.BoundedType.DOMINATING_REGION;
        lastObjectiveVal = new int[n];
        lowestUpperBounds = new int[n];
        this.findingFirstPoint = true;
        this.paretoFront = new ArrayList<>();
    }

    public void setBoundedType(GiaConfig.BoundedType boundedType) {
        this.boundedType = boundedType;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {

    }

    protected void computeDominatedArea() throws ContradictionException{
        boolean someLBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]){
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }else if (objectives[i].getLB() > lastObjectiveVal[i]){ // todo never set the propagator as passive until the problem is fixed, check the todo in setPassive()
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
        }//else{
        // all lower bounds are bigger than the last solution
        // todo check why is not working. For the instance vOptLib--ukp--2KP50-1A after lastObjectiveVal[0] == 877 && lastObjectiveVal[1] == 926, the propagator is set to passive and
        //  the next solution found is 890,949, but after that the propagator keeps inactive and a new solution is found with values 887, 995 which violates the constraint
        //this.setPassive();
        //}
    }

    protected void computeDominatedAreaSimple() throws ContradictionException {
        boolean someUBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]) {
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }
            if (objectives[i].getUB() > lastObjectiveVal[i]) { // todo never set the propagator as passive until the problem is fixed, check the todo in setPassive()
                someUBBiggerThanLastSolution = true;
            }
        }

        if (!someUBBiggerThanLastSolution) {
            fails();
        }
    }

    protected void setLowestUpperBound(){
        lowestUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            lowestUpperBounds[i] = computeLowestUpperBoundWithLastObjectiveVal(i);
        }
    }

    private int computeLowestUpperBoundWithLastObjectiveVal(int i){
        int lowestUpperBound = Integer.MAX_VALUE;
        int[] dominatingPoint = computeDominatingPointLastObjectiveVal(i);
        for (int[] sol : paretoFront) {
            if (dominates(dominatingPoint, sol)) {
                int currentPoint = sol[i] - 1;
                if (lowestUpperBound > currentPoint) {
                    lowestUpperBound = currentPoint;
                }
            }
        }
        return lowestUpperBound;
    }

    private int[] computeDominatingPointLastObjectiveVal(int i) {
        int[] dp = lastObjectiveVal.clone();
        dp[i] = Integer.MAX_VALUE;
        return dp;
    }

    /**
     * Return an int :
     * 0 if a doesn't dominate b
     * 1 if a dominates b
     *
     * @param a vector
     * @param b vector
     * @return a boolean representing the fact that a dominates b
     */
    protected boolean dominates(int[] a, int[] b) {
        for (int j = 0; j < objectives.length; j++) {
            if (a[j] < b[j]) return false;
        }
        return true;
    }

    @Override
    public ESat isEntailed() {
        return null;
    }

    @Override
    public void onSolution() {

    }

    public abstract void prepareGIAMaximizerForNextSolution();

    public abstract void prepareGIAMaximizerFirstSolution();

    public Solution getLastFeasibleSolution() {
        return lastSolution;
    }

    public void setLastSolution(Solution lastSolution) {
        this.lastSolution = lastSolution;
    }

    public int[] getLastObjectiveVal() {
        return lastObjectiveVal;
    }

//    TODO review, maybe it can be deleted-------------------
    public void setLastObjectiveVal(int[] lastObjectiveVal) {
        this.lastObjectiveVal = lastObjectiveVal;
    }
//    --------------------------------------------------------
}
