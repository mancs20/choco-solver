// File: MocoStrategy.java
package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.component.findsolution.FindNonDominatedSolutionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.InitialRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.objectivefunction.ObjectiveFunctionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.AddIntermediateSolutionsPreprocessing;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.PreprocessingStrategy;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.SelectRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.UpdateRegionsStrategy;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoSolutionDetails;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.*;
import java.util.stream.Stream;

public class MocoStrategy {
    private final InitialRegionStrategy initialRegion;
    private final List<PreprocessingStrategy> preprocessingList;
    private final ObjectiveFunctionStrategy objectiveFunction;
    private final SelectRegionStrategy selectRegion;
    private final FindNonDominatedSolutionStrategy findSolution;
    private final UpdateRegionsStrategy updateRegions;

    public MocoStrategy(StrategyComponents components) {
        this.initialRegion = components.getInitialRegion();
        this.preprocessingList = components.getPreprocessing();
        this.objectiveFunction = components.getObjectiveFunction();
        this.selectRegion = components.getSelectRegion();
        this.findSolution = components.getFindSolution();
        this.updateRegions = components.getUpdateRegion();
    }

    public ParetoSolutionDetails execute(Model model, IntVar[] objectives, boolean maximize, Criterion... stop) {
        // add stop critera if any
        model.getSolver().addStopCriterion(stop);
        //convert to maximization problem
        objectives = Stream.of(objectives).map(o -> maximize ? o : model.neg(o)).toArray(IntVar[]::new);
        ParetoArchive archive = new ParetoArchive(objectives);
        Set<Region> regionConstraints = initialRegion.computeInitialRegion(model, objectives);
        // preprocessing steps, could be a combination of multiple strategies
        StrategyParams params = new StrategyParams();
        params.setCheckIfNewSolutionDominates(true);
        params.setExhaustive(true);
        boolean hasAddIntermediateSolutions =
                preprocessingList.stream().anyMatch(p -> p instanceof AddIntermediateSolutionsPreprocessing);
        if (hasAddIntermediateSolutions) {
            params.setAddIntermediateSolutions(true);
        }

        for (PreprocessingStrategy step: preprocessingList) {
            step.apply(model, objectives, archive, params, stop);
        }
        // objective function if any
        if (params.getObjectiveFunction() != null && !params.isLexicographicOptimization()) {
            IntVar objVar = objectiveFunction.define(model, objectives, params);
            if (objVar != null) {
                model.setObjective(Model.MAXIMIZE, objVar);
                Constraint rawObj = params.getObjectiveFunction();
                rawObj.post();
            }
        }

        boolean checkDominanceWhenAdding;
        while (!regionConstraints.isEmpty() && !model.getSolver().isStopCriterionMet()) {
            Region region = selectRegion.select(regionConstraints);
            Solution s = findSolution.find(model, archive, objectives, region, params, stop);
            checkDominanceWhenAdding = params.isAddIntermediateSolutions() || params.isCheckIfNewSolutionDominates();
            archive.add(s, checkDominanceWhenAdding);
            updateRegions.update(regionConstraints, archive, objectives, s, params);
        }
        List<String> recorderList = params.getRecorderList();
        if (recorderList.size() == 0) {
            recorderList.add(model.getSolver().getMeasures().toString());
        }
        removeIdealSolution(archive, params, objectives);
        model.getSolver().removeStopCriterion(stop);
        return new ParetoSolutionDetails(archive.getParetoFrontSolutions(), recorderList, params.isExhaustive());
    }

    private void removeIdealSolution(ParetoArchive archive, StrategyParams params, IntVar[] objectives) {
        if (params.getIdealPoint() != null){
            if (!params.isExhaustive()) {
                addBestObjetiveValuesAsSolutionIfNotDomanited(archive, params, objectives);
                removeLastSolutionIfDominated(params, archive);
            } else {
                // remove from list put no solution
                List<String> recorderList = params.getRecorderList();
                int[] ideal = params.getIdealPoint();
                for (int i = 0; i < ideal.length; i++) {
                    recorderList.set(i, "No solution" + recorderList.get(i));
                }
            }
        }
    }

    private void addBestObjetiveValuesAsSolutionIfNotDomanited(ParetoArchive archive, StrategyParams params, IntVar[] objectives){
        int n = archive.getParetoFrontValues().get(0).length;
        Solution[] idealSolutions = params.getIdealSolutions();
        List<String> recorderList = params.getRecorderList();
        for (int i = 0; i < idealSolutions.length; i++) {
            int isDominated = -1;
            int[] valsIdeal = new int[n];
            for (int j = 0; j < n; j++) {
                valsIdeal[i] = idealSolutions[i].getIntVal(objectives[j]);
            }
            for (int j = 0; j < archive.size(); j++) {
                isDominated = archive.firstIsDominatedBySecond(valsIdeal, archive.getParetoFrontValues().get(j));
                if (isDominated >= 0) {
                    recorderList.set(i, "No solution" + recorderList.get(i));
                    break;
                }
            }
            if (isDominated < 0) {
                archive.getParetoFrontSolutions().add(i, idealSolutions[i]);
            }
        }
    }
    private void removeLastSolutionIfDominated(StrategyParams params, ParetoArchive archive){
        if (!params.isCheckIfNewSolutionDominates() && !params.isAddIntermediateSolutions()) {
            int[] lastSolutionVals = archive.getParetoFrontValues().get(archive.getParetoFrontValues().size() - 1);
            List<String> recorderList = params.getRecorderList();
            for (int i = archive.getParetoFrontValues().size()-2; i > -1; i--) {
                if (archive.firstIsDominatedBySecond(lastSolutionVals, archive.getParetoFrontValues().get(i)) >= 0){
                    archive.getParetoFrontValues().remove(archive.getParetoFrontValues().size() - 1);
                    recorderList.set(recorderList.size()-1, "No solution" + recorderList.get(recorderList.size()-1));
                    break;
                }
            }
        }
    }
}
