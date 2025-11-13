package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoArchive {
    private final List<int[]> paretoFront;
    private final List<Solution> paretoSolutions;

    private final IntVar[] objectives;
    private boolean canAddSolution;
    // Allow to recycle (dominated) Solution objects
    private final List<Solution> poolSols = new ArrayList<>();
    private final Model model;

    public ParetoArchive(IntVar[] objectives) {
        this.objectives = objectives;
        this.paretoFront = new ArrayList<>();
        this.paretoSolutions = new ArrayList<>();
        canAddSolution = true;
        model = this.objectives[0].getModel();
    }

    public List<Solution> getParetoFrontSolutions() {
        return paretoSolutions;
    }
    public List<int[]> getParetoFrontValues() {
        return paretoFront;
    }

    public void add(Solution solution, boolean checkDominanceWhenAdding) {
        if (solution != null && canAddSolution) {
            if (checkDominanceWhenAdding) {
                add(solution);
            } else {
                int[] vals = getSolutionObjVals(solution);
                addSolutionToArchive(solution, vals);
            }
        }
    }

    /**
     * Add a new solution to the archive. It removes all solutions that are dominated
     * by the new one.
     *
     * @param solution the candidate solution vector (int[] of objective values)
     */
    public void add(Solution solution) {
        if (solution != null && canAddSolution) {
            int[] vals = getSolutionObjVals(solution);
            if (noSimilarSolutionInArchive(vals)) {
                addSolutionToArchive(solution, vals);
            }
        }
    }

    /**
     * Add a new solution to the archive. It removes all solutions that are dominated
     * by the new one.
     *
     */
    public void addIntermediateSolutions() {
        int[] vals = getSolutionObjVals();
        if (noSimilarSolutionInArchive(vals)) {
            Solution solution;
            if (poolSols.isEmpty()) {
                solution = new Solution(model);
            } else {
                solution = poolSols.remove(poolSols.size() - 1);
            }
            solution.record();
            addSolutionToArchive(solution, vals);
        }
    }

    private boolean noSimilarSolutionInArchive(int[] vals) {
        int archiveSolIsDominated;
        boolean noSimilarSolution = true;
        for (int i = paretoSolutions.size() - 1; i >= 0; i--) {
            archiveSolIsDominated = firstIsDominatedBySecond(paretoFront.get(i), vals);
            if (archiveSolIsDominated > 0) {
                poolSols.add(paretoSolutions.remove(i));
                paretoFront.remove(i);
            } else if (archiveSolIsDominated == 0) {
                // is equal to a solution already in the archive
                noSimilarSolution = false;
                break;
            }
        }
        return noSimilarSolution;
    }

    private void addSolutionToArchive(Solution solution, int[] vals) {
        paretoSolutions.add(solution);
        paretoFront.add(vals);
    }

    public int[] getSolutionObjVals () {
        int[] vals = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            vals[i] = objectives[i].getValue();
        }
        return vals;
    }

    public int[] getSolutionObjVals (Solution solution) {
        int[] vals = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            vals[i] = solution.getIntVal(objectives[i]);
        }
        return vals;
    }

    public int firstIsDominatedBySecond(int[] archiveSolution, int[] vals) {
        int delta = 0;
        for (int i = 0; i < objectives.length; i++) {
            int deltaTmp = vals[i] - archiveSolution[i];
            if (deltaTmp < 0) {
                return -1;
            } else{
                delta += deltaTmp;
            }
        }
        return delta;
    }

    public boolean isEmpty() {
        return paretoSolutions.isEmpty();
    }

    public void setCanAddSolution(boolean canAddSolution) {
        this.canAddSolution = canAddSolution;
    }

    public Solution borrowDominatedSolutionFromPool() {
        if (poolSols.isEmpty()) {
            return new Solution(model);
        }
        // take from the end (cheaper remove)
        return poolSols.remove(poolSols.size() - 1);
    }

    public void returnDominatedSolutionToPool(Solution s) {
        // caller guarantees 's' is not stored in paretoSolutions
        poolSols.add(s);
    }
}
