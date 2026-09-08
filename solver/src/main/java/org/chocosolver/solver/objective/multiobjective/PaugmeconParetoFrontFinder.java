/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.UpdatablePropagator;
import org.chocosolver.solver.constraints.nary.lex.PropLexInt;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.limits.ICounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.*;

/**
 * Multi-objective algorithm robust for problems with 2 and 3 objectives. Combines ideas of SAUGMECON
 * (an epsilon-constraint method) with the Pareto global constraint. For better results use a randomized dynamic
 * branching strategies ensuring that the same enumeration tree is not constructed twice (we need a fresh search to
 * find each Pareto front point).
 * <p>
 * Based on <a href="https://doi.org/10.4230/LIPIcs.CP.2026.14">Combining an ε-Constraint Method with the
 * Pareto Global Constraint</a>, M. Combarro Simón, P. Talbot and P. Bouvry (CP 2026).
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public final class PaugmeconParetoFrontFinder extends ParetoFrontFinder {

    private List<CounterSnapshot> snapshots;
    private Solver solver;
    private IntVar[] objectives;
    private IdentityHashMap<int[], Integer> posByValsRef;
    private int[] bestLexFound = null;
    private int[] epsilon;

    @Override
    protected List<Solution> findParetoFront(
            Solver solver,
            IntVar[] unOrderedObjectives,
            Criterion... stop
    ) {
        this.solver = solver;
        this.objectives = orderObjectives(unOrderedObjectives);
        this.posByValsRef = new IdentityHashMap<>();
        this.bestLexFound = new int[this.objectives.length];
        this.epsilon = null;
        ParetoArchive archive = new ParetoArchive(objectives);
        ParetoMaximizer paretoPropagator = new ParetoMaximizer(objectives, false);
        paretoPropagator.setParetoFront(archive.getParetoFrontSolutions(), archive.getParetoFrontValues());
        Constraint paretoConstraint = new Constraint("PARETO", paretoPropagator);
        paretoConstraint.post();

        initializeStopCriteria(stop);

        int[] nadir = getNadirPoint();
        int[] ideal = getIdealPoint(archive, stop);
        if (ideal == null) {
            solver.getModel().unpost(paretoConstraint);
            return archive.getParetoFrontSolutions();
        }

        Constraint clint;
        UpdatablePropagator<int[]> plint;

        // Post a lexicographic constraint to find the best solution in the search space
        IntVar[] mobj = new IntVar[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            mobj[i] = solver.getModel().neg(this.objectives[i]) ;
        }
        plint = new PropLexInt(mobj, bestLexFound, true, true);
        //noinspection unchecked
        clint = new Constraint("lex objectives", (Propagator<IntVar>) plint);
        clint.post();

        int ubMainObj;
        List<SolutionEpsilonArrayInformation> previousSolutionInformation = new ArrayList<>();
        int[] previousSatSolIdxs = new int[]{-1, -1};

        epsilon = nadir.clone();
        int[] relativeWorstValue = ideal.clone();

        int[] solObjVals;

        while (epsilon[epsilon.length - 1] <= ideal[epsilon.length - 1]) {
            // reuse of previous solution
            ubMainObj = Integer.MAX_VALUE;
            previousSatSolIdxs[0] = -1;
            previousSatSolIdxs[1] = -1;
            ubMainObj = currentEpsilonSatisfiedWithPreviousSolution(epsilon, previousSolutionInformation, ubMainObj, previousSatSolIdxs);
            if (previousSatSolIdxs[0] != -1) {
                if (previousSatSolIdxs[1] != -1) {
                    solObjVals = previousSolutionInformation.get(previousSatSolIdxs[0]).getSolutions().get(previousSatSolIdxs[1]);
                } else {
                    solObjVals = null;
                }
            } else {
                solObjVals = solveGridPoint(ubMainObj, epsilon, paretoPropagator, archive, plint, previousSolutionInformation);
                if (solver.getSearchState() == SearchState.STOPPED) {
                    break;
                }
                resetSolver(stop);
            }
            updateEpsilon(solObjVals, epsilon, ideal, nadir, relativeWorstValue);
        }

        solver.getModel().unpost(clint);
        solver.getModel().unpost(paretoConstraint);
        return archive.getParetoFrontSolutions();
    }

    private int[] getNadirPoint() {
        int[] nadir = new int[objectives.length - 1];
        for (int i = 1; i < objectives.length; i++) {
            nadir[i - 1] = objectives[i].getLB();
        }
        return nadir;
    }

    private int[] getIdealPoint(ParetoArchive archive,
                                Criterion... stop) {
        int[] ideal = new int[objectives.length - 1];
        for (int i = 1; i < objectives.length; i++) {
            solver.getModel().setObjective(true, objectives[i]);
            Integer idealValue = null;
            while (solver.solve()) {
                archive.addIntermediateSolution();
                idealValue = objectives[i].getValue();
            }
            if (solver.getSearchState() == SearchState.STOPPED || idealValue == null) {
                return null;
            }
            if (i > 1) {
                // The true optimum for i-th objective may already be in the archive and therefore excluded by the
                // Pareto constraint.
                idealValue = getBestObjectiveValue(archive, i, idealValue);
            }
            resetSolver(stop);
            ideal[i - 1] = idealValue;
        }
        solver.getModel().clearObjective();
        if (solver.isDefaultSearchUsed()) {
            Search.defaultSearch(solver.getModel());
        }
        return ideal;
    }

    private Integer getBestObjectiveValue(ParetoArchive archive, int objectiveIndex, Integer best) {
        for (int[] values : archive.getParetoFrontValues()) {
            if (best == null || values[objectiveIndex] > best) {
                best = values[objectiveIndex];
            }
        }
        return best;
    }

    private int currentEpsilonSatisfiedWithPreviousSolution(int[] epsilon,
                                                              List<SolutionEpsilonArrayInformation> previousSolutionInformation,
                                                              int ubMainObj, int[] solObjValsIdxs) {
        if (previousSolutionInformation.isEmpty()) {
            return ubMainObj;
        }
        int idx = previousSolutionInformation.size() - 1;
        boolean solutionWithMoreRelaxationFound = false;
        int chosenSolIdx = -1;
        while (!solutionWithMoreRelaxationFound && idx > -1) {
            if (efArray1LessConstraintEfArray2(previousSolutionInformation.get(idx).getEfArray(), epsilon)) {
                SolutionEpsilonArrayInformation info = previousSolutionInformation.get(idx);
                solutionWithMoreRelaxationFound = true;
                if (info.isFeasible()) {
                    List<int[]> sols = info.getSolutions();
                    for (int s = 0; s < sols.size(); s++) {
                        int[] fSolutionValues = sols.get(s);
                        if (solutionSatisfyEfArr(fSolutionValues, epsilon)) {
                            chosenSolIdx = s;
                            break;
                        }
                    }
                    if (chosenSolIdx == -1) {
                        solutionWithMoreRelaxationFound = false;
                        idx -= 1;
                        if (ubMainObj > info.getSolutions().get(0)[0]) {
                            ubMainObj = info.getSolutions().get(0)[0];
                        }
                    }
                }
            } else {
                idx -= 1;
            }
        }
        solObjValsIdxs[0] = idx;
        solObjValsIdxs[1] = chosenSolIdx;

        return ubMainObj;
    }

    private static boolean efArray1LessConstraintEfArray2(int[] efArray1, int[] efArray2) {
        boolean lessConstrained = true;
        for (int i = 0; i < efArray1.length; i++) {
            if (efArray1[i] > efArray2[i]) {
                lessConstrained = false;
                break;
            }
        }
        return lessConstrained;
    }

    private static boolean solutionSatisfyEfArr(int[] solutionValues, int[] efArray) {
        boolean satisfy = true;
        for (int i = 0; i < efArray.length; i++) {
            if (solutionValues[i+1] < efArray[i]) {
                satisfy = false;
                break;
            }
        }
        return satisfy;
    }

    private int[] solveGridPoint(int ubMainObj, int[] epsilon, ParetoMaximizer paretoPropagator, ParetoArchive archive,
                                 UpdatablePropagator<int[]> plint,
                                 List<SolutionEpsilonArrayInformation> previousSolutionInformation) {

        // Add upper bound for main objective
        Constraint ubMainObjConstraint = null;
        if (ubMainObj != Integer.MAX_VALUE) {
            ubMainObjConstraint = solver.getModel().arithm(objectives[0], "<=", ubMainObj);
            ubMainObjConstraint.post();
        }

        // Add epsilon constraint
        List<Constraint> epsilonConstraints = postEpsilonConstraints(epsilon);

        // Use a temporary archive with only the relevant solutions for this grid point
        ParetoArchive localArchive = new ParetoArchive(objectives);
        int[] maxLexSol = findAllArchivePointsForEpsilon(epsilon, archive, localArchive);
        // wire paretoPropagator to local lists (so addIntermediateSolutions() updates what propagates)
        paretoPropagator.setParetoFront(localArchive.getParetoFrontSolutions(), localArchive.getParetoFrontValues());

        // Use lexicographic optimization to find the Pareto point
        Solution solution = lexMax(plint, paretoPropagator, maxLexSol, localArchive);

        int[] solutionObjVals = dealWithSolution(solution, epsilon, previousSolutionInformation, archive, localArchive);

        // unpost constraint related with this grid point
        if (ubMainObjConstraint != null) {
            solver.getModel().unpost(ubMainObjConstraint);
        }

        for (Constraint constraint : epsilonConstraints) {
            solver.getModel().unpost(constraint);
        }

        if (solver.getSearchState() == SearchState.STOPPED) {
            return null;
        } else {
            return solutionObjVals;
        }
    }

    private List<Constraint> postEpsilonConstraints(int[] epsilon) {
        List<Constraint> constraints = new ArrayList<>(epsilon.length);
        for (int i = 0; i < epsilon.length; i++) {
            Constraint constraint = solver.getModel().arithm(objectives[i + 1], ">=", epsilon[i]);
            constraint.post();
            constraints.add(constraint);
        }
        return constraints;
    }

    private int[] getObjectiveValues(Solution solution) {
        int[] values = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            values[i] = solution.getIntVal(objectives[i]);
        }
        return values;
    }

    private int[] findAllArchivePointsForEpsilon(int[] epsilon, ParetoArchive globalArchive, ParetoArchive localArchive) {
        int[] maxLexSol = new int[objectives.length];

        List<int[]> gf = globalArchive.getParetoFrontValues();
        ArrayList<Integer> idx = new ArrayList<>();

        for (int i = 0; i < gf.size(); i++) {
            int[] z = gf.get(i);
            if (inRegion(z, epsilon)) {
                idx.add(i);
                if (lexicographicallyGreater(z, maxLexSol)) {
                    maxLexSol = z;
                }
            }
        }

        for (int i : idx) {
            if (i < globalArchive.getCertifiedSize()) {
                localArchive.addCertified(null, gf.get(i));
            } else {
                posByValsRef.put(gf.get(i), i);
                localArchive.getParetoFrontValues().add(gf.get(i));
                localArchive.getParetoFrontSolutions().add(null);
            }
        }

        return maxLexSol;
    }

    // region defined by lower bounds on objectives[1...] using epsilon[0...]
    private static boolean inRegion(int[] objVals, int[] epsilon) {
        for (int i = 1; i < objVals.length; i++) {
            if (objVals[i] < epsilon[i - 1]) return false;
        }
        return true;
    }

    private Solution lexMax(UpdatablePropagator<int[]> plint, ParetoMaximizer paretoPropagator, int[] maxLexSol,
                            ParetoArchive localArchive) {
        boolean paretoPropagatorDisabled = false;

        // obtained the first solution without using the lexicographic ordering constraint
        ((PropLexInt) plint).setEnabled(false);
        for (int vIdx = 0; vIdx < objectives.length; vIdx++) {
            bestLexFound[vIdx] = Integer.MAX_VALUE;
        }
        plint.update(bestLexFound, false);

        Solution sol = null;
        while (solver.solve()) {
            sol = localArchive.addIntermediateSolution();

            // update bestLexFound
            for (int vIdx = 0; vIdx < objectives.length; vIdx++) {
                bestLexFound[vIdx] = objectives[vIdx].getValue();
            }

            if (!paretoPropagatorDisabled && lexicographicallyGreater(bestLexFound, maxLexSol)) {
                paretoPropagator.setEnabled(false);
                paretoPropagatorDisabled = true;
            }

            // enabled the propagator for lexicographic optimization after the first solution is obtained
            ((PropLexInt) plint).setEnabled(true);

            for (int vIdx = 0; vIdx < objectives.length; vIdx++) {
                bestLexFound[vIdx] = -bestLexFound[vIdx];
            }
            plint.update(bestLexFound, true);
        }
        if (paretoPropagatorDisabled) {
            paretoPropagator.setEnabled(true);
        }
        return sol;
    }

    private int[] dealWithSolution(Solution solution, int[] epsilon,
                                  List<SolutionEpsilonArrayInformation> previousSolutionInformation,
                                  ParetoArchive globalArchive, ParetoArchive localArchive) {
        int[] solutionObjectiveValues = null;
        if (solution == null) {
            // no solution in this region
            saveSolutionInformation(epsilon, null,  previousSolutionInformation);
            promoteAllNonCertifiedSeedsInRegion(globalArchive);
        } else {
            solutionObjectiveValues = getObjectiveValues(solution);
            List<int[]> solutionsToAddToEpsilon = new ArrayList<>(List.of(solutionObjectiveValues));
            List<int[]> solutionsThatCouldHaveBeenObtained = mergeLocalArchiveIntoGlobal(localArchive, globalArchive,
                    solution, solutionObjectiveValues);
            if (!solutionsThatCouldHaveBeenObtained.isEmpty()) {
                solutionsToAddToEpsilon.addAll(solutionsThatCouldHaveBeenObtained);
            }
            saveSolutionInformation(epsilon, solutionsToAddToEpsilon,  previousSolutionInformation);
        }
        return solutionObjectiveValues;
    }

    private static void saveSolutionInformation(int[] efArrayActual, List<int[]> solutionObjectiveValues,
                                                List<SolutionEpsilonArrayInformation> previousSolutionInformation) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEpsilonArrayInformation solutionEfArrayInformation;
        if (feasible && solutionObjectiveValues.size() == 1) {
            solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues.get(0), efArrayActual.clone(), true);
        } else {
            solutionEfArrayInformation = new SolutionEpsilonArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible);
        }
        previousSolutionInformation.add(solutionEfArrayInformation);
    }

    private void promoteAllNonCertifiedSeedsInRegion(ParetoArchive globalArchive) {
        while (!posByValsRef.isEmpty()) {
            Iterator<Map.Entry<int[], Integer>> it = posByValsRef.entrySet().iterator();
            Map.Entry<int[], Integer> e = it.next();
            int idx = e.getValue();
            int[] swappedIntoIdx = globalArchive.promoteToCertified(idx);
            it.remove();
            if (swappedIntoIdx != null) {
                Integer moved = posByValsRef.get(swappedIntoIdx);
                if (moved != null) {
                    posByValsRef.put(swappedIntoIdx, idx);
                }
            }
        }
    }

    private List<int[]> mergeLocalArchiveIntoGlobal(ParetoArchive localArchive, ParetoArchive globalArchive,
                                                    Solution optimalSolution, final int[] optimalValues) {

        //1- Remove surviving global solutions in the local archive, and Pareto certified if it is the case
        List<int[]> solutionsToAddToEpsilon = certifySurvivingGlobalSolutions(localArchive, globalArchive,
                optimalValues);

        //2- Remove dominated global solutions in the local archive
        removeDominatedGlobalSolutions(globalArchive);

        //3- add the new non-dominated solutions from the local archive to the global archive
        addNewLocalSolutions(localArchive, globalArchive, optimalSolution);

        return solutionsToAddToEpsilon;
    }

    private List<int[]> certifySurvivingGlobalSolutions(ParetoArchive localArchive, ParetoArchive globalArchive,
                                                        int[] optimalValues) {
        List<int[]> solutionsToAddToEpsilon = new ArrayList<>();
        List<Solution> localSols = localArchive.getParetoFrontSolutions();
        List<int[]> localVals = localArchive.getParetoFrontValues();
        for (int i = 0; i < localSols.size(); i++) {
            Solution sLoc = localSols.get(i);
            if (sLoc == null) {
                int[] valsLoc = localVals.get(i);
                if (lexicographicallyGreater(valsLoc, optimalValues)) {
                    solutionsToAddToEpsilon.add(valsLoc);
                    Integer gIdx = posByValsRef.get(valsLoc);
                    if (gIdx != null) {
                        int idxGlobal = gIdx;
                        int[] swappedIntoIdx = globalArchive.promoteToCertified(idxGlobal);
                        if (swappedIntoIdx != null) {
                            Integer moved = posByValsRef.get(swappedIntoIdx);
                            if (moved != null) {
                                posByValsRef.put(swappedIntoIdx, idxGlobal);
                            }
                        }
                    }
                }
                // remove the solution from the posByValsRef to know that should be kept in global archive
                posByValsRef.remove(valsLoc);
            }
        }
        return solutionsToAddToEpsilon;
    }

    private void removeDominatedGlobalSolutions(ParetoArchive globalArchive) {
        while (!posByValsRef.isEmpty()) {
            Iterator<Map.Entry<int[], Integer>> it = posByValsRef.entrySet().iterator();
            Map.Entry<int[], Integer> e = it.next();
            int idx = e.getValue();
            int[] swappedIntoIdx = globalArchive.removeAndSwapWithLast(idx);
            it.remove();
            if (swappedIntoIdx != null) {
                Integer moved = posByValsRef.get(swappedIntoIdx);
                if (moved != null) {
                    posByValsRef.put(swappedIntoIdx, idx);
                }
            }
        }
    }

    private void addNewLocalSolutions(ParetoArchive localArchive, ParetoArchive globalArchive, Solution optimalSolution) {
        globalArchive.addCertified(optimalSolution);
        for (Solution localSolution : localArchive.getParetoFrontSolutions()) {
            if (localSolution != null && localSolution != optimalSolution) {
                globalArchive.addSolution(localSolution);
            }
        }
    }

    static void updateEpsilon(int[] solutionValues, int[] epsilon, int[] ideal, int[] nadir,
                              int[] relativeWorstValue) {
        if (solutionValues == null) {
            skipInfeasibleRegion(epsilon, ideal, nadir);
        } else {
            relativeWorstValue[0] = solutionValues[1];
            for (int i = 1; i < relativeWorstValue.length; i++) {
                relativeWorstValue[i] = Math.min(relativeWorstValue[i], solutionValues[i + 1]);
            }
        }

        for (int i = 0; i < epsilon.length; i++) {
            if (epsilon[i] < ideal[i] && relativeWorstValue[i] < ideal[i]) {
                epsilon[i] = relativeWorstValue[i] + 1;
                relativeWorstValue[i] = ideal[i];
                return;
            }
            if (i < epsilon.length - 1) {
                epsilon[i] = nadir[i];
            } else {
                epsilon[i] = ideal[i] + 1;
            }
        }
    }

    private static void skipInfeasibleRegion(int[] epsilon, int[] ideal, int[] nadir) {
        int index = epsilon.length - 1;
        for (int i = 0; i < epsilon.length - 1; i++) {
            if (epsilon[i] != nadir[i]) {
                index = i;
                break;
            }
        }
        System.arraycopy(ideal, 0, epsilon, 0, index + 1);
    }

    int[] getCurrentEpsilon() {
        return epsilon == null ? null : epsilon.clone();
    }

    private IntVar[] orderObjectives(IntVar[] objectives) {
        int mainObjective = 0;
        long largestRange = getRange(objectives[0]);

        for (int i = 1; i < objectives.length; i++) {
            long range = getRange(objectives[i]);
            if (range > largestRange) {
                largestRange = range;
                mainObjective = i;
            }
        }

        IntVar[] ordered = new IntVar[objectives.length];
        ordered[0] = objectives[mainObjective];

        int target = 1;
        for (int i = 0; i < objectives.length; i++) {
            if (i != mainObjective) {
                ordered[target++] = objectives[i];
            }
        }

        return ordered;
    }

    private long getRange(IntVar objective) {
        return (long) objective.getUB() - objective.getLB();
    }

    private void resetSolver(Criterion... stop) {
        saveCurrentStopCriteriaValues();
        solver.reset();
        reapplyStopCriteria(stop);
    }

    private void initializeStopCriteria(Criterion... stop) {
        snapshots = new ArrayList<>();
        for (Criterion c : stop) {
            if (c instanceof ICounter) {
                snapshots.add(new CounterSnapshot((ICounter) c));
            }
            solver.addStopCriterion(c);
        }
    }

    private void saveCurrentStopCriteriaValues() {
        for (CounterSnapshot snapshot : snapshots) {
            snapshot.updateRemainingLimit();
        }
    }

    private void reapplyStopCriteria(Criterion... stop) {
        for (Criterion criterion : stop) {
            if (!(criterion instanceof ICounter)) {
                solver.addStopCriterion(criterion);
            }
        }
        for (CounterSnapshot snapshot : snapshots) {
            snapshot.reapplyToSolver(solver);
        }
    }

    private boolean lexicographicallyGreater(int[] arrToTest, int[] baselineArr) {
        for (int i = 0; i < arrToTest.length; i++) {
            if (arrToTest[i] > baselineArr[i]) {
                return true;
            } else if (arrToTest[i] < baselineArr[i]) {
                return false;
            }
        }
        return false;
    }
}

class SolutionEpsilonArrayInformation {
    private final List<int[]> solutions;
    private final int[] epsilonArr;
    private final boolean feasible;

    public SolutionEpsilonArrayInformation(int[] solution, int[] epsilonArr, boolean feasible) {
        this.solutions = Collections.singletonList(solution);
        this.epsilonArr = epsilonArr;
        this.feasible = feasible;
    }

    public SolutionEpsilonArrayInformation(List<int[]> solutions, int[] epsilonArr, boolean feasible) {
        this.solutions = solutions;
        this.epsilonArr = epsilonArr;
        this.feasible = feasible;
    }

    public int[] getSolution() {
        return solutions.get(0);
    }

    public List<int[]> getSolutions() {
        return solutions;
    }

    public int[] getEfArray() {
        return epsilonArr;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
