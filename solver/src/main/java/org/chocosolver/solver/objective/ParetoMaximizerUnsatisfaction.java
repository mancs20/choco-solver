/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2023, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.ParetoFeasibleRegion;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.List;

/**
 * Class to store the pareto front (multi-objective optimization).
 * <p>
 * Based on "Multi-Objective Large Neighborhood Search", P. Schaus , R. Hartert (CP'2013)
 * </p>
 *
 * @author Charles Vernerey
 * @author Charles Prud'homme
 * @author Jean-Guillaume Fages
 * @author Jani Simomaa
 */
public class ParetoMaximizerUnsatisfaction extends Propagator<IntVar> implements IMonitorSolution {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************
    // objective function
    private final IntVar[] objectives;
    private final int n;

    // varaibles for one point
    private int[] lastObjectiveVal;
    private int[] upperRegionCorner;
    private List<int[]> forbiddenValues;
    private boolean firstPropagation = true;
    private Solution lastSolution;
    private boolean firstPartialSolution = false;
    private final boolean portfolio;

    private final Model model;

    //***********************************************************************************
    // CONSTRUCTOR
    //***********************************************************************************

    /**
     * Create an object to compute the Pareto front of a multi-objective problem.
     * Objectives are expected to be maximized (use {@link org.chocosolver.solver.variables.IViewFactory#intView(int, IntVar, int)} in case of minimisation).
     * <p>
     * Maintain the set of dominating solutions and
     * posts constraints dynamically to prevent search from computing dominated ones.
     * <p>
     * The Solutions store decision variables (those declared in the search strategy)
     * BEWARE: requires the objectives to be declared in the search strategy
     *
     * @param objectives objective variables (must all be optimized in the same direction)
     */
    public ParetoMaximizerUnsatisfaction(final IntVar[] objectives, boolean portfolio) {
        super(objectives, PropagatorPriority.LINEAR, false);
        this.objectives = objectives.clone();
        n = objectives.length;
        model = this.objectives[0].getModel();
        this.setLastSolution(new Solution(model));
        this.portfolio = portfolio;
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void onSolution() {
        firstPartialSolution = true;
        // get objective values
        boolean saveSolution = true;
        if (portfolio) {
            saveSolution = saveSolutionPortfolio();
        }
        if (saveSolution) {
            for (int i = 0; i < n; i++) {
                lastObjectiveVal[i] = objectives[i].getValue();
            }
            setLastSolution(new Solution(model));
            lastSolution.record();
        }
    }

    private boolean saveSolutionPortfolio() {
        boolean saveSolution = true;
        for (int i = 0; i < n; i++) {
            if (objectives[i].getValue() < lastObjectiveVal[i]) {
                saveSolution = false;
                break;
            }
        }
        return saveSolution;
    }

//    todo verify for other problems if it is better to use the propagation condition or not, for the test case it is
//     better NOT to use it
     //@Override
//    public int getPropagationConditions(int vIdx) {
//        return IntEventType.boundAndInst();
//    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (firstPropagation) {
            firstPropagation = false;
            applyInitialFiltering();
        }
        if (firstPartialSolution) {
            computeDominatedArea();
        }
    }

    private void applyInitialFiltering() throws ContradictionException{
        // check upper bound and lb
        for (int i = 0; i < n; i++) {
            if (objectives[i].getUB() > upperRegionCorner[i]){
                objectives[i].updateUpperBound(upperRegionCorner[i], this);
            }
            // check lower bound
            if (objectives[i].getLB() < lastObjectiveVal[i]){
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }
        }
        // check forbidden values
        if (forbiddenValues != null){
            for (int[] forbiddenValue : forbiddenValues){
                for (int i = 0; i < n; i++) {
                    if (objectives[i].contains(forbiddenValue[i])){
                        objectives[i].removeValue(forbiddenValue[i], this);
                    }
                }
            }
        }
    }

    private void computeDominatedArea() throws ContradictionException{
        boolean someLBoundBigger = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]){
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }else if (objectives[i].getLB() > lastObjectiveVal[i]){ // todo never set the propagator as passive until the problem is fixed, check the todo in setPassive()
                someLBoundBigger = true;
            }
        }

        if (!someLBoundBigger){
            //all the lower bounds are equal to the last solution, there should be at least one upper bound that is bigger
            //if not then fails
            boolean atLeastOneBigger = false;
            int onlyOneBiggerIdx = -1;
            // check that current solution is bigger than the last one
            for (int i = 0; i < n; i++) {
                if (objectives[i].getUB() > lastObjectiveVal[i]){
                    if (atLeastOneBigger && onlyOneBiggerIdx != -1){
                        onlyOneBiggerIdx = -1;
                    }else{
                        atLeastOneBigger = true;
                        onlyOneBiggerIdx = i;
                    }
                }
            }
            if (atLeastOneBigger){
                if (onlyOneBiggerIdx != -1){
                    // only one objective has bigger upper bound than the last solution
                    if (objectives[onlyOneBiggerIdx].getLB() < lastObjectiveVal[onlyOneBiggerIdx] + 1){
                        // update the lower bound of the only one bigger
                        objectives[onlyOneBiggerIdx].updateLowerBound(lastObjectiveVal[onlyOneBiggerIdx] + 1, this);
                    }
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


    @Override
    public ESat isEntailed() {
        boolean undefined = false;
        for (int i = 0; i < n; i++) {
            if (objectives[i].getUB() < lastObjectiveVal[i]) {
                if (portfolio){
                    return ESat.TRUE;
                }
                System.out.println("UB: " + objectives[i].getUB() + " < " + lastObjectiveVal[i]);
                return ESat.FALSE;
            }
        }
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() <= lastObjectiveVal[i]) {
                undefined = true;
                break;
            }
        }
        if (undefined) {
            return ESat.UNDEFINED;
        }else {
            return ESat.TRUE;
        }
    }

    public Solution getLastFeasibleSolution() {
        return lastSolution;
    }

    public int[] getLastObjectiveVal() {
        return lastObjectiveVal;
    }

    public void configureInitialUbLb(ParetoFeasibleRegion feasible_region){
        this.upperRegionCorner = feasible_region.getUpperCorner();
        this.lastObjectiveVal = feasible_region.getLowerCorner().clone();
        firstPropagation = true;
        firstPartialSolution = false;
        if (feasible_region.getEfficientCorners().size() > 0) {
            forbiddenValues = feasible_region.getEfficientCorners();
        }
    }

    public void setLastSolution(Solution lastSolution) {
        this.lastSolution = lastSolution;
    }

    public int[] getObjectivesValue() {
        int[] objectivesValues = new int[n];
        for (int i = 0; i < n; i++) {
            objectivesValues[i] = objectives[i].getValue();
        }
        return objectivesValues;
    }

    public void setLastObjectiveVal(int[] lastObjectiveVal) {
        this.lastObjectiveVal = lastObjectiveVal;
    }
}
