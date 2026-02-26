/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2026, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Explained;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.Arrays;
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
@Explained(comment = "must be tested")
public class ParetoMaximizer extends Propagator<IntVar> implements IMonitorSolution {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    // Set of incomparable and Pareto-best solutions
    private List<Solution> paretoSolutions;
    private List<int[]> paretoFront;

    private final Model model;

    // Allow to recycle (dominated) Solution objects
    private final List<Solution> poolSols = new ArrayList<>();

    // objective function
    private final IntVar[] objectives;
    private final int n;

    //private final int[] vals;
    // DEBUG (public, quick-and-dirty): times in nanoseconds
    public long timeFindingTightestPoint = 0L;
    public long timeAddingSolutionToArchiveRemovingDominates = 0L;
    public int sameSolutionReached = 0;


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
    public ParetoMaximizer(final IntVar[] objectives) {
        super(objectives, PropagatorPriority.QUADRATIC, false);
        this.paretoSolutions = new ArrayList<>();
        this.paretoFront = new ArrayList<>();
        this.objectives = objectives.clone();
        n = objectives.length;
        model = objectives[0].getModel();
        //vals = new int[n];
    }

    public void setSharedFront(List<Solution> sharedParetoSolutions, List<int[]> sharedParetoValues) {
        this.paretoSolutions = sharedParetoSolutions;
        this.paretoFront = sharedParetoValues;
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    /**
     * @return the set of Pareto-best (possibly optimal) solutions found so far
     */
    public List<Solution> getParetoFront() {
        return paretoSolutions;
    }

    public List<int[]> getParetoFrontValues() {
        return paretoFront;
    }

    @Override
    public void onSolution() {
        // get objective values
        int[] vals = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            vals[i] = objectives[i].getValue();
        }
        long __t0_remove = System.nanoTime();
        // remove dominated solutions
        for (int i = paretoFront.size() - 1; i >= 0; i--) {
            if (isDominated(paretoSolutions.get(i), vals)) {
                poolSols.add(paretoSolutions.remove(i));
                paretoFront.remove(i);
            }
        }
        timeAddingSolutionToArchiveRemovingDominates += System.nanoTime() - __t0_remove;
        // store current solution
        Solution solution;
        if (poolSols.isEmpty()) {
            solution = new Solution(model);
        } else {
            solution = poolSols.remove(0);
        }
        solution.record();
        paretoSolutions.add(solution);
        paretoFront.add(vals);
    }

    private boolean isDominated(Solution solution, int[] vals) {
        for (int i = 0; i < n; i++) {
            int delta = solution.getIntVal(objectives[i]) - vals[i];
            if (delta > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (paretoFront.size() > 0) {
            for (int i = 0; i < objectives.length; i++) {
                computeTightestPoint(i);
            }
        }
    }

    /**
     * Compute tightest point for objective i
     * i.e. the point that dominates DP_i and has the biggest obj_i
     *
     * @param i index of the variable
     */
    private void computeTightestPoint(int i) throws ContradictionException {
        // tightest point can not be calculated if paretoFront is empty
        if (paretoFront.size() > 0) {
            int tightestPoint = Integer.MIN_VALUE;
            int[] dominatedPoint = computeDominatedPoint(i);
            long __t0_finding_tightest_point = System.nanoTime();
            for (int[] sol : paretoFront) {
                int dominates = dominates(sol, dominatedPoint, i);
                if (dominates > 0) {
//                    int currentPoint = dominates == 1 ? sol[i] : sol[i] + 1; // if there are multiple equal solutions
//                    and one of them is already in the Pareto front, it is possible that another one is found and
//                    replace the existing one in the front. This is counterproductive, as the code is designed to store
//                    just one solution for each Pareto point.
                    // todo check when dominates == 2 if the lower bound cannot be increased then get out
                    int currentPoint = sol[i] + 1;
                    if (tightestPoint < currentPoint) {
                        tightestPoint = currentPoint;
                    }
                    if (!lcg() && objectives[i].getUB() < tightestPoint) {
                        break;
                    }
                }
            }
            timeFindingTightestPoint += System.nanoTime() - __t0_finding_tightest_point;
            if (tightestPoint > Integer.MIN_VALUE) {
                if (lcg()){
                    objectives[i].updateLowerBound(tightestPoint, this, explainRaiseLB(i));
                } else {
                    objectives[i].updateLowerBound(tightestPoint, this);
                }
            }
        }
    }

    /**
     * Compute dominated point for objective i,
     * i.e. DP_i = (obj_1_max,...,obj_i_min,...,obj_m_max)
     *
     * @param i index of the variable
     * @return dominated point
     */
    private int[] computeDominatedPoint(int i) {
        int[] dp = new int[objectives.length];
        for (int j = 0; j < objectives.length; j++) {
            dp[j] = objectives[j].getUB();
        }
        dp[i] = objectives[i].getLB();
        return dp;
    }

    /**
     * Return an int :
     * 0 if a doesn't dominate b
     * 1 if a dominates b and a = b if we don't take into account index i
     * 2 if a dominates b and a dominates b if we don't take into account index i
     *
     * @param a vector
     * @param b vector
     * @param i index
     * @return an int representing the fact that a dominates b
     */
    private int dominates(int[] a, int[] b, int i) {
        if (Arrays.equals(a, b)) {
            sameSolutionReached++;
            return 2;
        }
        int dominates = 0;
        for (int j = 0; j < objectives.length; j++) {
            if (a[j] < b[j]) return 0;
            if (a[j] > b[j]) {
                if (dominates == 0) dominates = 1;
                if (j != i) dominates = 2;
            }
        }
        return dominates;
    }

    private Reason explainRaiseLB(int i) {
        if (!lcg()) return Reason.undef();
        int[] ps;
        ps = new int[n];
        int m = 1;
        for (int j = 0; j < n; j++) {
            if (j == i) continue;
            ps[m++] = objectives[j].getMaxLit(); // (obj_j <= UB_j)
        }
        ps[0] = 0; // reserved slot / asserting lit placeholder
        return Reason.r(ps);
    }

    @Override
    public ESat isEntailed() {
        return ESat.TRUE;
    }
}
