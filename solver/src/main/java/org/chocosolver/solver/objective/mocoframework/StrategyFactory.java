package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.objective.mocoframework.component.findsolution.*;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.*;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.*;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.*;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.*;
import org.chocosolver.solver.objective.mocoframework.enums.*;

public class StrategyFactory {

    public static InitialRegionStrategy getInitialRegion(InitialRegionType type) {
        InitialRegionStrategy strategy;
        if (type == InitialRegionType.ENTIRE_OBJECTIVE_SPACE) {
            strategy = new WholeObjectiveRegion();
        } else {
            throw new IllegalArgumentException("Unknown InitialRegionType: " + type);
        }
        return strategy;
    }

    public static PreprocessingStrategy getPreprocessing(PreprocessingType type) {
        PreprocessingStrategy strategy;
        switch (type) {
            case GAVANELLI:
                strategy = new GavanelliPrepro();
                break;
            case SAUGMECON:
                strategy = new SaugmeconPreprocessing();
                break;
            default:
                throw new IllegalArgumentException("Unknown PreprocessingType: " + type);
        }
        return strategy;
    }

    public static SelectRegionStrategy getSelectRegion(SelectRegionType type) {
        SelectRegionStrategy strategy;
        switch (type) {
            case SINGLE:
                strategy = new SingleRegionSelector();
                break;
            default:
                throw new IllegalArgumentException("Unknown SelectRegionType: " + type);
        }
        return strategy;
    }

    public static FindNonDominatedSolutionStrategy getFindSolution(FindSolutionType type) {
        FindNonDominatedSolutionStrategy strategy;
        switch (type) {
            case SOLVE:
                strategy = new SolveStrategy();
                break;
            case OPTIMIZE:
                strategy = new OptimizeStrategy();
                break;
            case OPTIMIZE_PARETO_GLOBAL_CONSTRAINT:
                strategy = new OptimizeParetoGlobalStrategy();
                break;
            default:
                throw new IllegalArgumentException("Unknown FindSolutionType: " + type);
        }
        return strategy;
    }

    public static UpdateRegionsStrategy getUpdateRegion(UpdateRegionType type) {
        UpdateRegionsStrategy strategy;
        switch (type) {
            case GAVANELLI:
                strategy = new GavanelliUpdate();
                break;
            case SAUGMECON:
                strategy = new SaugmeconUpdate();
                break;
            default:
                throw new IllegalArgumentException("Unknown UpdateRegionType: " + type);
        }
        return strategy;
    }
}
