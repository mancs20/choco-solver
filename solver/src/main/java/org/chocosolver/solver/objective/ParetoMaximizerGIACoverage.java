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

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.ParetoFeasibleRegion;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;


public class ParetoMaximizerGIACoverage extends ParetoMaximizerGIAGeneral {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************
    private final boolean initialFiltering;
    private int[] upperRegionCorner;
    private boolean firstPropagation = true;
    private boolean firstPartialSolution = false;

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
    public ParetoMaximizerGIACoverage(final IntVar[] objectives, boolean portfolio, PropagatorPriority priority) {
        super(objectives, priority, false, portfolio);
        this.setLastSolution(new Solution(model));
        this.initialFiltering = true;
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void onSolution() {
        // todo add lazy bound
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
        // todo comment firstPropagation
        if (firstPropagation) {
            firstPropagation = false;
            if (initialFiltering) {
                applyInitialFiltering();
            }
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

    public void prepareGIAMaximizerFirstSolution() {

    }

    public void prepareGIAMaximizerForNextSolution(){
        configureInitialUbLb();
        setLastSolution(null);
    }

    public void configureInitialUbLb(ParetoFeasibleRegion feasible_region){
        this.lastObjectiveVal = feasible_region.getLowerCorner().clone();
        this.upperRegionCorner = feasible_region.getUpperCorner().clone();
        firstPropagation = true;
        firstPartialSolution = false;
    }

    public void configureInitialUbLb(){
        // todo if config criteria is not none change the value of lastObjectiveVal
        this.lastObjectiveVal = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            lastObjectiveVal[i] = objectives[i].getLB();
        }
        firstPropagation = true;
        firstPartialSolution = false;
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
