package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class ParetoArchive {
    private final List<int[]> paretoFront;
    private final List<Solution> paretoSolutions;

    private final IntVar[] objectives;
    private int[] lastAddedSolutionObjValues;

    public ParetoArchive(IntVar[] objectives) {
        this.objectives = objectives.clone();
        this.paretoFront = new ArrayList<>();
        this.paretoSolutions = new ArrayList<>();
        lastAddedSolutionObjValues = null;
    }

    public List<Solution> getParetoFrontSolutions() {
        return paretoSolutions;
    }
    public List<int[]> getParetoFrontValues() {
        return paretoFront;
    }

    public void add(Solution solution, boolean checkDominanceWhenAdding) {
        if (solution != null) {
            if (checkDominanceWhenAdding) {
                add(solution);
            } else {
                int[] vals = new int[objectives.length];
                if(differentFromLast(vals, solution)){
                    lastAddedSolutionObjValues = vals;
                    paretoSolutions.add(solution);
                    paretoFront.add(vals);
                }
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
        int isDominated = -1;
        if (solution != null) {
            int[] vals = new int[objectives.length];
            if (differentFromLast(vals, solution)) {
                for (int i = paretoSolutions.size() - 1; i >= 0; i--) {
                    isDominated = firstIsDominatedBySecond(paretoFront.get(i), vals);
                    if (isDominated > 0) {
                        paretoSolutions.remove(i);
                        paretoFront.remove(i);
                    } else if (isDominated == 0) {
                        break;
                    }
                }
                if (isDominated != 0) {
                    paretoSolutions.add(solution);
                    paretoFront.add(vals);
                }
                lastAddedSolutionObjValues = vals;
            }
        }
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

    private boolean differentFromLast(int[] vals, Solution solution) {
        boolean equalToLast = true;
        for (int i = 0; i < objectives.length; i++) {
            vals[i] = solution.getIntVal(objectives[i]);
            if (lastAddedSolutionObjValues == null || vals[i] != lastAddedSolutionObjValues[i]) {
                equalToLast = false;
            }
        }
        return !equalToLast;
    }

    public void clear() {
        paretoSolutions.clear();
    }

    public boolean isEmpty() {
        return paretoSolutions.isEmpty();
    }

    public int size() {
        return paretoSolutions.size();
    }

    public void setLastAddedSolutionObjValues(int[] lastAddedSolutionObjValues) {
        this.lastAddedSolutionObjValues = lastAddedSolutionObjValues;
    }
}
