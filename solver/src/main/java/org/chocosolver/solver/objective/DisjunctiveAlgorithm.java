package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.moexperiments.TimeoutHolder;

import java.util.*;

public class DisjunctiveAlgorithm extends ParetoAbstract implements TimeoutHolder {

    private final Model model;
    private final Solver solver;
    private final IntVar[] objectives;
    private final float timeout;
    private final long startTime;

    private int[] bestObjectiveValues;
    private Solution[] bestObjectiveValuesSolution;
    private boolean stopCriterionReached;
    protected int solveCallsCount;

    public DisjunctiveAlgorithm(Model model, IntVar[] objectives, int timeoutSec) {
        solveCallsCount = 0;
        startTime = System.nanoTime();
        this.model = model;
        this.solver = model.getSolver();
        this.objectives = objectives;
        this.timeout = timeoutSec;
    }

    public void findParetoFront() {
        getIdealPoint();
        if (!stopCriterionReached) {
            prepareObjectiveFunction();
        }
        List<Region> feasibleRegions = new ArrayList<>();
        Set<Region> infeasibleRegions = new HashSet<>();
        int[] initialRegion = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            initialRegion[i] = objectives[i].getLB() - 1;
        }
        feasibleRegions.add(new Region(initialRegion, objectives));
        Constraint[] regionConstraints = new Constraint[objectives.length];
        while (!feasibleRegions.isEmpty() && !stopCriterionReached) {
            Region region = selectRegion(feasibleRegions);
            // post region constraints
            for (int i = 0; i < objectives.length; i++) {
                regionConstraints[i] = model.arithm(objectives[i], ">", region.v[i]);
                regionConstraints[i].post();
            }
            // optimize in the region
            Solution solution = optimizeIntVar();
            // unpost region constraints
            for (Constraint c : regionConstraints) {
                model.unpost(c);
            }
            // add solution to the Pareto set
            if (solution != null) {
                solutions.add(solution);
            }
            // update regions
            feasibleRegions = updateRegions(feasibleRegions, infeasibleRegions, region, solution);
        }
        postProcessResults(feasibleRegions);
    }

    private void prepareObjectiveFunction() {
        int LBsum = 0;
        int UBsum = 0;
        for (int i = 0; i < objectives.length; i++) {
            LBsum += objectives[i].getLB();
            UBsum += bestObjectiveValues[i];
        }
        IntVar objectiveSum = model.intVar("objectiveSum", LBsum, UBsum);
        model.sum(objectives, "=", objectiveSum).post();
        model.setObjective(true, objectiveSum);
    }

    private void getIdealPoint() {
        bestObjectiveValuesSolution = new Solution[objectives.length];
        bestObjectiveValues = findSolutionsConsideringOneObjective();
    }

    private Region selectRegion(List<Region> feasibleRegions) {
        return feasibleRegions.get(0);
    }

    private List<Region> updateRegions(List<Region> feasibleRegions, Set<Region> infeasibleRegions, Region region,
                                       Solution solution){
        if (solution == null) {
            infeasibleRegions.add(region);
            feasibleRegions.remove(region);
        } else {
            feasibleRegions = getAllNewRegions(feasibleRegions, solution);
            filterDominatingRegions(feasibleRegions);
            discardInfeasibleRegions(feasibleRegions, infeasibleRegions);
        }

        return feasibleRegions;
    }

    private List<Region> getAllNewRegions(List<Region> feasibleRegions, Solution solution) {
        List<Region> newRegions = new ArrayList<>();
        int[] objectivesValues = solutionToObjectivesValues(solution);
        for (Region region: feasibleRegions){
            newRegions.addAll(combineRegionWithSolution(region.v, objectivesValues));
        }
        return newRegions;
    }

    private List<Region> combineRegionWithSolution(int[] region, int[] objectivesValues) {
        List<Region> newRegions = new ArrayList<>();
        for (int i = 0; i < objectives.length; i++) {
            int[] newRegion = new int[region.length];
            System.arraycopy(region,0, newRegion, 0, region.length);
            newRegion[i] = Math.max(objectivesValues[i], region[i]);
            newRegions.add(new Region(newRegion, objectives));
        }
        return newRegions;
    }

    private void filterDominatingRegions(List<Region> feasibleRegions) {
        for (int i = 0; i < feasibleRegions.size() - 1; i++) {
            for (int j = i+1; j < feasibleRegions.size(); j++) {
                boolean iDominatesJ = true;
                boolean jDominatesI = true;
                for (int k = 0; k < objectives.length; k++) {
                    if (feasibleRegions.get(i).v[k] > feasibleRegions.get(j).v[k]){
                        iDominatesJ = false;
                    }else if (feasibleRegions.get(i).v[k] < feasibleRegions.get(j).v[k]){
                        jDominatesI = false;
                    }
                    if (!iDominatesJ && !jDominatesI) break;
                }
                if (iDominatesJ) {
                    feasibleRegions.remove(j);
                    j--;
                } else if (jDominatesI) {
                    feasibleRegions.remove(i);
                    i--;
                    break;
                }
            }
        }
    }

    private void discardInfeasibleRegions(List<Region> feasibleRegions, Set<Region> infeasibleRegions) {
        List<Region> toRemoveFromFeasible = new ArrayList<>();
        List<Region> toAddToInfeasible = new ArrayList<>();

        for (Region region : feasibleRegions) {
            boolean regionViolatesIdeal = false;
            for (int k = 0; k < objectives.length; k++) {
                if (region.v[k] >= bestObjectiveValues[k]) {
                    regionViolatesIdeal = true;
                    break;
                }
            }
            if (regionViolatesIdeal) {
                toAddToInfeasible.add(region);
                toRemoveFromFeasible.add(region);
                continue;
            }
            for (Region infeasibleRegion : infeasibleRegions) {
                boolean infeasibleRegionContainsRegion = true;
                for (int k = 0; k < objectives.length; k++) {
                    if (region.v[k] < infeasibleRegion.v[k]) {
                        infeasibleRegionContainsRegion = false;
                        break;
                    }
                }
                if (infeasibleRegionContainsRegion) {
                    toRemoveFromFeasible.add(region);
                    break;
                }
            }
        }

        feasibleRegions.removeAll(toRemoveFromFeasible);
        infeasibleRegions.addAll(toAddToInfeasible);
    }

    private void postProcessResults(List<Region> feasibleRegions) {
        exhaustive = feasibleRegions.isEmpty();
        if (exhaustive) {
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                recorderList.set(i, "Preprocessing solution" + recorderList.get(i));
            }
        } else {
            addBestObjectiveValuesAsSolutionIfNotDominated();
            removeLastSolutionIfDominated();
        }
    }

    private void addBestObjectiveValuesAsSolutionIfNotDominated(){
        if (!solutions.isEmpty()) {
            int[][] objectiveValuesIdealSolution = new int[bestObjectiveValuesSolution.length][];
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                objectiveValuesIdealSolution[i] = solutionToObjectivesValues(bestObjectiveValuesSolution[i]);
            }
            boolean isDominatedByParetoPoint = true;
            int insertIndex = 0;
            for (int i = 0; i < objectiveValuesIdealSolution.length; i++) {
                for (Solution s: solutions) {
                    int[] paretoPoint = solutionToObjectivesValues(s);
                    isDominatedByParetoPoint = true;
                    for (int j = 0; j < objectives.length; j++) {
                        if (paretoPoint[j] < objectiveValuesIdealSolution[i][j]) {
                            isDominatedByParetoPoint = false;
                            break;
                        }
                    }
                    if (isDominatedByParetoPoint) {
                        recorderList.set(i, "Preprocessing solution" + recorderList.get(i));
                        break;
                    }
                }
                if (!isDominatedByParetoPoint) {
                        solutions.add(insertIndex, bestObjectiveValuesSolution[i]);
                        insertIndex++;
                }
            }
        } else {
            for (Solution solution : bestObjectiveValuesSolution) {
                if (solution != null) {
                    solutions.add(solution);
                }
            }
        }
    }

    private void removeLastSolutionIfDominated(){
        int[] lastSolutionVals = solutionToObjectivesValues(solutions.get(solutions.size()-1));
        boolean dominated;
        for (int i = solutions.size()-2; i > -1; i--) {
            int[] paretoPoint = solutionToObjectivesValues(solutions.get(i));
            dominated = true;
            for (int j = 0; j < objectives.length; j++) {
                if (lastSolutionVals[j] > paretoPoint[j]) {
                    dominated = false;
                    break;
                }
            }
            if (dominated){
                solutions.remove(solutions.size()-1);
                recorderList.set(recorderList.size()-1, "Dominated solution" + recorderList.get(recorderList.size()-1));
                break;
            }
        }
    }

    private int[] findSolutionsConsideringOneObjective() {
        if (stopCriterionReached) {
            return new int[]{};
        }
        int[] objectivesValues = new int[objectives.length];
        for (int i = 0; i < objectives.length; i++) {
            IntVar objective = objectives[i];
            model.setObjective(true, objective);
            Solution solution = optimizeIntVar();
            if (solution != null) {
                objectivesValues[i] = solution.getIntVal(objective);
                bestObjectiveValuesSolution[i] = solution;
                model.arithm(objective, "<=", objectivesValues[i]).post();
            } else {
                break;
            }
        }
        return objectivesValues;
    }

    private Solution optimizeIntVar() {
        Solution solution = null;
        float remainingTimeout = updateSolverTimeoutCurrentTime(solver, timeout, startTime);
        if (remainingTimeout == 0) {
            stopCriterionReached = true;
        }else{
            if (!solver.isStopCriterionMet()) {
                solution = new Solution(model);
                while (solver.solve()){
                    solution.record();
                }
                solver.removeStopCriterion();
                recorderList.add(solver.getMeasures().toString());

                if (!solution.exists()) solution = null;
                if (!solver.isStopCriterionMet()){
                    solver.reset();
                    solveCallsCount++;
                    solver.getMeasures().setRestartCount(solveCallsCount);
                }else {
                    stopCriterionReached = true;
                }
            }
        }
        return solution;
    }

    private int[] solutionToObjectivesValues(Solution solution) {
        int[] objectivesValues = new int[objectives.length];
        if (solution == null) {
            return objectivesValues;
        }
        for (int i = 0; i < objectives.length; i++) {
            objectivesValues[i] = solution.getIntVal(objectives[i]);
        }
        return objectivesValues;
    }
}

final class Region {
    final int[] v;
    private final int hash;

    Region(int[] values, IntVar[] objectives) {
        this.v = new int[objectives.length];
        if (values.length < objectives.length) {
            System.arraycopy(values, 0, this.v, 0, values.length);
            for (int i = values.length; i < objectives.length; i++) {
                this.v[i] = objectives[i].getLB() - 1;
            }
        } else {
            System.arraycopy(values, 0, this.v, 0, objectives.length);
        }
        this.hash = Arrays.hashCode(v);  // cached
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Region)) return false;
        Region r = (Region) o;
        return Arrays.equals(v, r.v);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}



