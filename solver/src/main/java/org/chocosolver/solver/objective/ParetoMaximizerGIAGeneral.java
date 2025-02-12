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
    protected int[] highestCurrentUpperBounds;
    protected int[] originalUpperBounds;
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
        highestCurrentUpperBounds = new int[n];
        this.findingFirstPoint = true;
        this.paretoFront = new ArrayList<>();
        originalUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            originalUpperBounds[i] = objectives[i].getUB();
        }
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
        } else{
            // all lower bounds are equal or greater than the last solution with at least one lower bound greater than
            // the last solution
            this.setPassive();
        }
    }

    protected void computeDominatedAreaSimple() throws ContradictionException {
        boolean someUBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]) {
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }
            if (objectives[i].getUB() > lastObjectiveVal[i]) {
                someUBBiggerThanLastSolution = true;
            }
        }

        if (!someUBBiggerThanLastSolution) {
            fails();
        }
    }

    protected void setLowestUpperBound(){
        highestCurrentUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            highestCurrentUpperBounds[i] = computeLowestUpperBoundWithLastObjectiveVal(i);
        }
    }

    private int computeLowestUpperBoundWithLastObjectiveVal(int i){
        int[] dominatingPoint = computeDominatingPointLastObjectiveVal(i);
        return computeLowestUBToAvoidDomination(dominatingPoint, i);
    }

    protected int computeLowestUBToAvoidDomination(int[] dominatingPoint, int i) {
        int highestPossibleUpperBound = Integer.MAX_VALUE;
        for (int[] sol : paretoFront) {
            if (dominates(dominatingPoint, sol)) {
                int currentPoint = sol[i] - 1;
                if (highestPossibleUpperBound > currentPoint) {
                    highestPossibleUpperBound = currentPoint;
                }
            }
        }
        return highestPossibleUpperBound;
    }

    private int[] computeDominatingPointLastObjectiveVal(int i) {
        int[] dp = lastObjectiveVal.clone();
        dp[i] = originalUpperBounds[i];
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

    public abstract void prepareGIAMaximizerForNextSolution(int[] newParetoPointToAdd);

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
