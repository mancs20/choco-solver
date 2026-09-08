/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the non-dominated solutions found by a multi-objective algorithm.
 * Objectives are expected to be maximized.
 * <p>
 * The archive can also distinguish certified Pareto-optimal solutions from
 * intermediate non-dominated solutions. Certified solutions are kept at the
 * beginning of the archive.
 * <p>
 * Candidates passed to {@link #addIntermediateSolution()} or {@link #addSolution(Solution)}
 * must already be known not to be dominated by an archived solution. These methods remove
 * existing solutions dominated by the candidate, but do not reject a candidate dominated
 * by an existing solution. Certified solutions are added without a dominance comparison.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public final class ParetoArchive {

    private final List<int[]> paretoFront;
    private final List<Solution> paretoSolutions;
    private final List<Solution> solutionPool;
    private final IntVar[] objectives;
    private final Model model;
    private int certifiedSize;

    /**
     * Creates an empty archive.
     *
     * @param objectives maximization objectives
     */
    public ParetoArchive(IntVar[] objectives) {
        this.objectives = objectives.clone();
        this.paretoFront = new ArrayList<>();
        this.paretoSolutions = new ArrayList<>();
        this.solutionPool = new ArrayList<>();
        this.model = objectives[0].getModel();
    }

    /**
     * Add the last solution obtained to the archive. Archived intermediate solutions dominated by the new solution are
     * removed.
     * The current solution must not be dominated by an archived solution.
     *
     * @return the recorded solution, or {@code null} when it was already present and similar solutions are not saved.
     */
    public Solution addIntermediateSolution() {
        int[] values = getCurrentObjectiveValues();
        if (checkDominanceVsExistingSolutions(values)) {
            Solution solution = borrowSolution();
            solution.record();
            add(solution, values);
            return solution;
        } else {
            return null;
        }
    }

    /**
     * Adds an already-recorded solution to the archive.
     * Solutions dominated by it are removed.
     * The supplied solution must not be dominated by an archived solution.
     *
     * @param solution solution to add
     * @return {@code true} if the solution was added, or {@code false} if an
     *  equivalent solution was already present
     */
    public boolean addSolution(Solution solution) {
        int[] values = getSolutionObjVals(solution);
        if (checkDominanceVsExistingSolutions(values)) {
            add(solution, values);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a solution known to be Pareto optimal.
     * No dominance comparison is required because the solution is already certified.
     *
     * @param solution solution to add
     */
    public void addCertified(Solution solution) {
        addCertified(solution, getSolutionObjVals(solution));
    }

    /**
     * Adds a solution known to be Pareto optimal.
     * No dominance comparison is required because the solution is already certified.
     *
     * @param solution solution to add
     * @param values objective values associated with {@code solution}
     */
    public void addCertified(Solution solution, int[] values) {
        if (checkDominanceVsExistingSolutions(values)) {
            add(solution, values.clone());
            swap(certifiedSize, paretoSolutions.size() - 1);
            certifiedSize++;
        }
    }

    private void add(Solution solution, int[] values) {
        paretoSolutions.add(solution);
        paretoFront.add(values);
    }

    /**
     * Promotes an intermediate solution to certified Pareto-optimal solution.
     *
     * @param idx index of the solution to promote
     * @return objective values of the promoted solution, or {@code null} if it was already certified
     */
    public int[] promoteToCertified(int idx) {
        if (idx < certifiedSize) return null; // already Pareto-optimal certified
        int target = certifiedSize;
        swap(idx, target);
        certifiedSize++;
        return (idx == target) ? null : paretoFront.get(idx);
    }

    /**
     * @return solutions in the archive
     */
    public List<Solution> getParetoFrontSolutions() {
        return paretoSolutions;
    }

    /**
     * @return objective vectors in the same order as {@link #getParetoFrontSolutions()}
     */
    public List<int[]> getParetoFrontValues() {
        return paretoFront;
    }

    /**
     * @return number of certified solutions at the beginning of the archive
     */
    public int getCertifiedSize() {
        return certifiedSize;
    }


    /**
     * Checks if a new solution dominates any existing solutions in the archive. The new solution is guaranteed to be
     * non-dominated, but it is possible that some of the solutions in the archive are dominated by it. In that case,
     * they are removed from the archive and recycled.
     *
     * @param newSolObjVals values of the new solution to check against the archive solutions
     * @return {@code true} if the new solution is not equivalent (same objectives values) to an existing solution,
     * {@code false} if it is equivalent to an existing solution and should not be added to the archive.
     */
    private boolean checkDominanceVsExistingSolutions(int[] newSolObjVals) {
        for (int i = paretoFront.size() - 1; i >= certifiedSize; i--) {
            int relation = dominanceCheck(newSolObjVals, paretoFront.get(i));
            if (relation == 0) {
                return false;
            } else if (relation == 1) {
                recycle(paretoSolutions.remove(i));
                paretoFront.remove(i);
            }
        }
        return true;
    }

    /**
     * @return {@code 1} if the first dominates the second, {@code 0} if they are
     * equivalent (same objectives values), or {@code -1} if the first do not dominate the second.
     */
    private int dominanceCheck(int[] first, int[] second) {
        boolean firstIsBetter = false;
        for (int i = 0; i < objectives.length; i++) {
            if (first[i] < second[i]) {
                return -1;
            } else if (first[i] > second[i]) {
                firstIsBetter = true;
            }
        }
        return firstIsBetter ? 1 : 0;
    }

    private int[] getCurrentObjectiveValues() {
        int[] values = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            values[i] = objectives[i].getValue();
        }
        return values;
    }

    private int[] getSolutionObjVals(Solution solution) {
        int[] values = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            values[i] = solution.getIntVal(objectives[i]);
        }
        return values;
    }

    private Solution borrowSolution() {
        return solutionPool.isEmpty()
                ? new Solution(model)
                : solutionPool.remove(solutionPool.size() - 1);
    }

    private void recycle(Solution solution) {
        if (solution != null) {
            solutionPool.add(solution);
        }
    }

    public int[] removeAndSwapWithLast(int idx) {
        int last = paretoSolutions.size() - 1;
        if (idx < certifiedSize || idx > last) {
            throw new IndexOutOfBoundsException("idx=" + idx + "certifiedSize=" + certifiedSize + ", size=" + paretoSolutions.size());
        }
        if (idx != last) {
            swap(idx, last);
        }
        Solution removed = paretoSolutions.remove(last);
        paretoFront.remove(last);
        if (removed != null) solutionPool.add(removed);
        return (idx == last) ? null : paretoFront.get(idx);
    }

    private void swap(int first, int second) {
        if (first == second) {
            return;
        }
        Solution solution = paretoSolutions.get(first);
        paretoSolutions.set(first, paretoSolutions.get(second));
        paretoSolutions.set(second, solution);

        int[] values = paretoFront.get(first);
        paretoFront.set(first, paretoFront.get(second));
        paretoFront.set(second, values);
    }
}
