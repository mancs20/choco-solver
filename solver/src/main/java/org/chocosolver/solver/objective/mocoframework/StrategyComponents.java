package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.objective.mocoframework.component.findsolution.FindNonDominatedSolutionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.InitialRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.PreprocessingStrategy;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.SelectRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.UpdateRegionsStrategy;

import java.util.List;

public class StrategyComponents {

    private final InitialRegionStrategy initialRegion;
    private final List<PreprocessingStrategy> preprocessing;
    private final SelectRegionStrategy selectRegion;
    private final FindNonDominatedSolutionStrategy findSolution;
    private final UpdateRegionsStrategy updateRegion;

    public StrategyComponents(InitialRegionStrategy initialRegion,
                              List<PreprocessingStrategy> preprocessing,
                              SelectRegionStrategy selectRegion,
                              FindNonDominatedSolutionStrategy findSolution,
                              UpdateRegionsStrategy updateRegion) {
        this.initialRegion = initialRegion;
        this.preprocessing = preprocessing;
        this.selectRegion = selectRegion;
        this.findSolution = findSolution;
        this.updateRegion = updateRegion;

        validateCompatibility();
    }

    private void validateCompatibility() {
        if (preprocessing == null || preprocessing.isEmpty()) {
            throw new IllegalArgumentException("At least one preprocessing strategy is required.");
        }

        // example compatibility check
        //        if (findSolution instanceof OptimizeParetoGlobalStrategy) {
//            if (!(updateRegion instanceof GavanelliUpdate || updateRegion instanceof SaugmeconUpdate)) {
//                throw new IllegalArgumentException("OptimizeParetoGlobalStrategy requires a compatible UpdateRegions strategy.");
//            }
//        }

        // Add any other rules here
    }

    public InitialRegionStrategy getInitialRegion() {
        return initialRegion;
    }

    public List<PreprocessingStrategy> getPreprocessing() {
        return preprocessing;
    }

    public SelectRegionStrategy getSelectRegion() {
        return selectRegion;
    }

    public FindNonDominatedSolutionStrategy getFindSolution() {
        return findSolution;
    }

    public UpdateRegionsStrategy getUpdateRegion() {
        return updateRegion;
    }
}
