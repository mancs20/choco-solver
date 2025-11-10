package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.objective.mocoframework.StrategyComponents;
import org.chocosolver.solver.objective.mocoframework.StrategyFactory;
import org.chocosolver.solver.objective.mocoframework.component.findsolution.FindNonDominatedSolutionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.InitialRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.objectivefunction.ObjectiveFunctionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.PreprocessingStrategy;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.SelectRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.UpdateRegionsStrategy;
import org.chocosolver.solver.objective.mocoframework.enums.*;

import java.util.ArrayList;
import java.util.List;

public class StrategyParser {

    public static StrategyComponents parse(String strategyName) {
        if (!strategyName.startsWith("MocoStrate-")) {
            throw new IllegalArgumentException("Strategy must start with MocoStrate-");
        }

        String[] parts = strategyName.substring("MocoStrate-".length()).split("-");
        InitialRegionStrategy initialRegion = null;
        List<PreprocessingStrategy> preprocessingList = new ArrayList<>();
        SelectRegionStrategy selectRegion = null;
        FindSolutionType findSolutionType = null;
        ObjectiveFunctionType objectiveFunctionType = null;
        UpdateRegionsStrategy updateRegions = null;

        StrategyFactory strategyFactory = new StrategyFactory();
        for (String part : parts) {
            if (part.startsWith("Init_")) {
                String key = part.substring(5).toUpperCase();
                initialRegion = strategyFactory.getInitialRegion(InitialRegionType.valueOf(key));
            } else if (part.startsWith("Prepro_")) {
                String[] keys = part.substring(7).split("_");
                for (String key : keys) {
                    preprocessingList.add(strategyFactory.getPreprocessing(PreprocessingType.valueOf(key.toUpperCase())));
                }
            } else if (part.startsWith("ObjFun_")) {
                String key = part.substring(7).toUpperCase();
                objectiveFunctionType = ObjectiveFunctionType.valueOf(key);
            }
            else if (part.startsWith("Sel_")) {
                String key = part.substring(4).toUpperCase();
                selectRegion = strategyFactory.getSelectRegion(SelectRegionType.valueOf(key));
            } else if (part.startsWith("FindSol_")) {
                String key = part.substring(8).toUpperCase();
                findSolutionType = FindSolutionType.valueOf(key);
            } else if (part.startsWith("Update_")) {
                String key = part.substring(7).toUpperCase();
                updateRegions = strategyFactory.getUpdateRegion(UpdateRegionType.valueOf(key));
            } else {
                throw new IllegalArgumentException("Unknown strategy part: " + part);
            }
        }

        ObjectiveFunctionStrategy objectiveFunction =
                strategyFactory.getObjectiveFunctionOrDefault(objectiveFunctionType);
        FindNonDominatedSolutionStrategy findSolution = strategyFactory.getFindSolutionOrDefault(findSolutionType);

        // check for missing components
        if (initialRegion == null) {
            throw new IllegalArgumentException("InitialRegion component is missing.");
        }
        if (preprocessingList.isEmpty()) {
            throw new IllegalArgumentException("Preprocessing component is missing.");
        }
        if (selectRegion == null) {
            throw new IllegalArgumentException("SelectRegion component is missing.");
        }
        if (updateRegions == null) {
            throw new IllegalArgumentException("UpdateRegions component is missing.");
        }

        return new StrategyComponents(
                initialRegion,
                preprocessingList,
                objectiveFunction,
                selectRegion,
                findSolution,
                updateRegions);
    }
}
