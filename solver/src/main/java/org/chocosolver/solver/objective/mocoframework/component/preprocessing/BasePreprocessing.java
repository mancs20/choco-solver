package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class BasePreprocessing implements PreprocessingStrategy {
    protected final SolutionFinder optimizer;

    protected BasePreprocessing(SolutionFinder optimizer) {
        this.optimizer = optimizer;
    }

    protected int[] getIdealValues(IntVar[] objectives, ParetoArchive archive, Set<Integer> excludedObjectivesId, StrategyParams params, Criterion... stop) {
        int n = objectives.length;

        List<Solution> idealSolutions = new ArrayList<>();
        List<Integer> idealValues = new ArrayList<>();
        Region dummyRegion = new Region();
        boolean objectiveFunctionInParamsArg = true;
        if (!params.isOptimization()) {
            params.setUseOptimization(true);
            objectiveFunctionInParamsArg = false;
        }

        boolean disableParetoMaximizerAfterFirstSolution = params.isDisableParetoMaximizerAfterFirstSolution();
        params.setDisableParetoMaximizerAfterFirstSolution(false);

        Model model = objectives[0].getModel();
        for (int i = 0; i < n; i++) {
            if (excludedObjectivesId.contains(i)) {
                continue;
            }
            IntVar objective = objectives[i];
            model.setObjective(true, objective);
            Solution opt = optimizer.find(model, archive, objectives, dummyRegion, params, stop);
            if (opt != null) {
                // if Pareto Global constraint is used, the solution returned by the optimizer is not necessarily the
                // one corresponding to the ideal point, so we have to compare it with the one in the archive and take
                // the best one for that objective
                Solution sol;
                if (params.getParetoMaximizer() != null && params.isAddIntermediateSolutions()) {
                    sol = getIdealSolutionConsideringArchive(opt, archive, objectives, i);
                } else {
                    sol = opt.copySolution();
                }
                idealSolutions.add(sol);
                idealValues.add(sol.getIntVal(objectives[i]));
            } else {
                if (!model.getSolver().isStopCriterionMet()) {
                    if (params.getParetoMaximizer() == null) {
                        throw new RuntimeException("Preprocessing failed: unable to find ideal point. The problem should be feasible.");
                    } else {
                        // The Pareto front was found
                        System.out.println("Preprocessing: Pareto front found while looking for ideal point of " +
                                "the first " + i + " objectives without considering objectives " +
                                Arrays.toString(excludedObjectivesId.toArray()) +
                                ". Marking the search as exhaustive.");
                        params.setExhaustive(true);
                    }
                }
                fillRemainingIdeals(i, objectives, excludedObjectivesId, idealValues, idealSolutions);
                break;
            }
        }
        params.setIdealSolutions(idealSolutions.toArray(new Solution[0]));
        if (!objectiveFunctionInParamsArg) {
            params.setUseOptimization(false);
        }
        params.setDisableParetoMaximizerAfterFirstSolution(disableParetoMaximizerAfterFirstSolution);
        return idealValues.stream().mapToInt(Integer::intValue).toArray();
    }

    private Solution getIdealSolutionConsideringArchive(Solution opt, ParetoArchive archive, IntVar[] objectives, int objectiveIndex) {
        Solution bestSol = opt;
        int bestValue = opt.getIntVal(objectives[objectiveIndex]);

        List<int[]> frontVals = archive.getParetoFrontValues();
        List<Solution> frontSols = archive.getParetoFrontSolutions();
        for (int i = 0; i < frontVals.size(); i++) {
            int objectiveVal = frontVals.get(i)[objectiveIndex];
            if (objectiveVal > bestValue) {
                bestValue = objectiveVal;
                bestSol = frontSols.get(i);
            }
        }
        return bestSol.copySolution();
    }

    private void fillRemainingIdeals(int startIdx, IntVar[] objectives, Set<Integer> excludedObjectivesId,
                                     List<Integer> idealValues,
                                     List<Solution> idealSolutions) {
        int n = objectives.length;
        for (int j = startIdx; j < n; j++) {
            if (excludedObjectivesId.contains(j)) continue;
            idealValues.add(objectives[j].getLB());
            idealSolutions.add(null);
        }
    }



    protected int[] getNadirValues(IntVar[] objectives, Set<Integer> excludedObjectivesId, Criterion... stop) {
        int n = objectives.length;

        List<Integer> nadirValues = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (excludedObjectivesId.contains(i)) {
                continue;
            }
            nadirValues.add(objectives[i].getLB());
        }
        // slower way to get tight nadir values
//        Region dummyRegion = new Region();
//        for (int i = 1; i < objectives.length; i++) {
//            IntVar objective = objectives[i];
//            model.setObjective(false, objective);
//            Solution sol = optimizer.find(model, objectives, dummyRegion);
//            if (sol != null) {
//                nadirObjectiveValues[i - 1] = sol.getIntVal(objective);
//            } else {
//                // todo handle timeout. All the problems should be feasible
//                break;
//            }
//        }
        return nadirValues.stream().mapToInt(Integer::intValue).toArray();
    }
}

