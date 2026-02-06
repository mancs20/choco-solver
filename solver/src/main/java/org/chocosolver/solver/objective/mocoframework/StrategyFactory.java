package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.objective.mocoframework.component.findsolution.*;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.*;
import org.chocosolver.solver.objective.mocoframework.component.objectivefunction.NoneObjectiveFunction;
import org.chocosolver.solver.objective.mocoframework.component.objectivefunction.ObjectiveFunctionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.objectivefunction.SumObjectiveFunctionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.*;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.*;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.*;
import org.chocosolver.solver.objective.mocoframework.enums.*;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;

public class StrategyFactory {

    private final SolutionFinder sharedSolutionFinder;

    public StrategyFactory() {
        this.sharedSolutionFinder = new SolutionFinder(); // One shared instance
    }

    public InitialRegionStrategy getInitialRegion(InitialRegionType type) {
        InitialRegionStrategy strategy;
        if (type == InitialRegionType.ENTIRE_OBJECTIVE_SPACE) {
            strategy = new EntireObjectiveRegion();
        } else {
            throw new IllegalArgumentException("Unknown InitialRegionType: " + type);
        }
        return strategy;
    }

    public PreprocessingStrategy getPreprocessing(PreprocessingType type) {
        PreprocessingStrategy strategy;
        switch (type) {
            case GAVANELLI:
                strategy = new GavanelliPrepro();
                break;
            case SAUGMECON:
                strategy = new SaugmeconPreprocessing(this.sharedSolutionFinder);
                break;
            case ADD_INTERMEDIATE_SOLUTIONS:
                strategy = new AddIntermediateSolutionsPreprocessing();
                break;
            case NO_GOOD_ON_INTERMEDIATE_SOLUTIONS:
                strategy = new NoGoodOnIntermediateSolution();
                break;
            case USE_OBJECTIVE_MANAGER_FOR_OBJECTIVES:
                strategy = new ObjectiveManagerWithObjLB();
                break;
            default:
                throw new IllegalArgumentException("Unknown PreprocessingType: " + type);
        }
        return strategy;
    }

    public ObjectiveFunctionStrategy getObjectiveFunction(ObjectiveFunctionType type) {
        switch (type) {
            case SUM:
                return new SumObjectiveFunctionStrategy();
            case NONE:
                return new NoneObjectiveFunction();
            default:
                throw new IllegalArgumentException("Unknown ObjectiveFunctionType: " + type);
        }
    }

    public ObjectiveFunctionStrategy getObjectiveFunctionOrDefault(ObjectiveFunctionType type) {
        if (type == null || type == ObjectiveFunctionType.NONE) {
            return new NoneObjectiveFunction();
        } else {
            return getObjectiveFunction(type); // extendable later
        }
    }

    public SelectRegionStrategy getSelectRegion(SelectRegionType type) {
        SelectRegionStrategy strategy;
        if (type == SelectRegionType.SINGLE) {
            strategy = new SingleRegionSelector();
        } else {
            throw new IllegalArgumentException("Unknown SelectRegionType: " + type);
        }
        return strategy;
    }

    public FindNonDominatedSolutionStrategy getFindSolution(FindSolutionType type) {
        FindNonDominatedSolutionStrategy strategy;
        switch (type) {
            case GENERIC:
                strategy = new DefaultFindSolutionStrategy(this.sharedSolutionFinder);
                break;
            case SAUGMECON:
                strategy = new SaugmeconFindSolution(this.sharedSolutionFinder);
                break;
            case GIA:
                strategy = new GIAOptSumFindSolution(this.sharedSolutionFinder);
                break;
            default:
                throw new IllegalArgumentException("Unknown FindSolutionType: " + type);
        }
        return strategy;
    }

    public FindNonDominatedSolutionStrategy getFindSolutionOrDefault(FindSolutionType type) {
        if (type == null || type == FindSolutionType.GENERIC) {
            return new DefaultFindSolutionStrategy(this.sharedSolutionFinder);
        } else {
            return getFindSolution(type); // allows throwing if you later add more types
        }
    }

    public UpdateRegionsStrategy getUpdateRegion(UpdateRegionType type) {
        UpdateRegionsStrategy strategy;
        switch (type) {
            case GAVANELLI:
                strategy = new GavanelliUpdate();
                break;
            case SAUGMECON:
                strategy = new SaugmeconUpdate();
                break;
            case GIA:
                strategy = new GIAUpdate();
                break;
            default:
                throw new IllegalArgumentException("Unknown UpdateRegionType: " + type);
        }
        return strategy;
    }
}
