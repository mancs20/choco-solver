package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.tools.ArrayUtils;

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
    private final Variable[] varsToStore;

    // number of Pareto optimal solutions in the archive
    private int certifiedSize = 0;

    public ParetoArchive(IntVar[] objectives) {
        this.objectives = objectives;
        this.paretoFront = new ArrayList<>();
        this.paretoSolutions = new ArrayList<>();
        canAddSolution = true;
        model = this.objectives[0].getModel();

        Variable[] decisionVars = (Variable[]) model.getHook("decisionVariables");
        if (decisionVars != null) {
            this.varsToStore = ArrayUtils.append(decisionVars, objectives);
        } else {
            System.out.println("No decision variables hooked in the model. Saving only objectives in the model");
            this.varsToStore = objectives;
        }
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
    public Solution addIntermediateSolutions() {
        int[] vals = getSolutionObjVals();
        if (noSimilarSolutionInArchive(vals)) {
            Solution solution;
            if (poolSols.isEmpty()) {
                solution = createNewSolution();
            } else {
                solution = poolSols.remove(poolSols.size() - 1);
            }
            solution.record();
            addSolutionToArchive(solution, vals);
            return solution;
        }
        return null;
    }

    /**
     * Add a solution to the archive that is guaranteed to be Pareto optimal
     * @param solution new solution to add to the archive
     * @param vals objective values of the solution
     */
    public void addCertified(Solution solution, int[] vals) {
        if (noSimilarSolutionInArchive(vals)) {
            addSolutionToArchive(solution, vals);
            swap(certifiedSize, paretoSolutions.size() - 1);
            certifiedSize++;
        }
    }

    public void addCertified(Solution solution) {
        int[] vals = getSolutionObjVals(solution);
        addCertified(solution, vals);
    }

    public int[] promoteToCertified(int idx) {
        if (idx < certifiedSize) return null;
        int target = certifiedSize;
        swap(idx, target);
        certifiedSize++;
        return (idx == target) ? null : paretoFront.get(idx);
    }

    private boolean noSimilarSolutionInArchive(int[] vals) {
        int archiveSolIsDominated;
        boolean noSimilarSolution = true;
        for (int i = paretoSolutions.size() - 1; i >= certifiedSize; i--) {
            archiveSolIsDominated = firstIsDominatedBySecond(paretoFront.get(i), vals);
            if (archiveSolIsDominated > 0) {
                Solution removed = paretoSolutions.remove(i); // could be null if it was seeded
                paretoFront.remove(i);
                if (removed != null) poolSols.add(removed);   // recycle only real solutions
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
            return createNewSolution();
        }
        // take from the end (cheaper remove)
        return poolSols.remove(poolSols.size() - 1);
    }

    public void returnDominatedSolutionToPool(Solution s) {
        // caller guarantees 's' is not stored in paretoSolutions
        poolSols.add(s);
    }

    public List<int[]> getParetoFront() {
        return paretoFront;
    }

    public int[] removeAtSwap(int idx) {
        int last = paretoSolutions.size() - 1;
        if (idx < certifiedSize || idx > last) {
            throw new IndexOutOfBoundsException("idx=" + idx + "certifiedSize=" + certifiedSize + ", size=" + paretoSolutions.size());
        }
        if (idx != last) {
            swap(idx, last);
        }
        Solution removed = paretoSolutions.remove(last);
        paretoFront.remove(last);
        if (removed != null) poolSols.add(removed);
        return (idx == last) ? null : paretoFront.get(idx);
    }

    private void swap(int i, int j) {
        if (i == j) return;
        Solution si = paretoSolutions.get(i);
        paretoSolutions.set(i, paretoSolutions.get(j));
        paretoSolutions.set(j, si);

        int[] vi = paretoFront.get(i);
        paretoFront.set(i, paretoFront.get(j));
        paretoFront.set(j, vi);
    }

    public int getCertifiedSize() {
        return certifiedSize;
    }

    private Solution createNewSolution() {
        return new Solution(model, varsToStore);
    }
}
