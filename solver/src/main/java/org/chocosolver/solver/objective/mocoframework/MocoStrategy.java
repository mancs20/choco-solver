// File: MocoStrategy.java
package org.chocosolver.solver.objective.mocoframework;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;

import org.chocosolver.solver.objective.mocoframework.component.findsolution.FindNonDominatedSolutionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.initialregion.InitialRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.preprocessing.PreprocessingStrategy;
import org.chocosolver.solver.objective.mocoframework.component.selectregion.SelectRegionStrategy;
import org.chocosolver.solver.objective.mocoframework.component.updateregions.UpdateRegionsStrategy;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class MocoStrategy {
    private final InitialRegionStrategy initialRegion;
    private final List<PreprocessingStrategy> preprocessingList;
    private final SelectRegionStrategy selectRegion;
    private final FindNonDominatedSolutionStrategy findSolution;
    private final UpdateRegionsStrategy updateRegions;

    public MocoStrategy(StrategyComponents components) {
        this.initialRegion = components.getInitialRegion();
        this.preprocessingList = components.getPreprocessing();
        this.selectRegion = components.getSelectRegion();
        this.findSolution = components.getFindSolution();
        this.updateRegions = components.getUpdateRegion();
    }

    public List<Solution> execute(Model model, IntVar[] objectives, boolean maximize) {
        //convert to maximization problem
        objectives = Stream.of(objectives).map(o -> maximize ? o : model.neg(o)).toArray(IntVar[]::new);
        ParetoArchive archive = new ParetoArchive(objectives);
        Set<Region> regionConstraints = initialRegion.computeInitialRegion(model, objectives);
        // preprocessing steps, could be a combination of multiple strategies
        Map<String, Object> params = new HashMap<>();
        for (PreprocessingStrategy step: preprocessingList) {
            Map<String, Object> singleParams = step.apply(model, objectives, archive);
            params.putAll(singleParams);
        }

        while (!regionConstraints.isEmpty()) {
            Region region = selectRegion.select(regionConstraints);
            Solution s = findSolution.find(model, objectives, region);
            archive.add(s);
            updateRegions.update(regionConstraints, archive, objectives, s, params);
        }

        return archive.getParetoFrontSolutions();
    }
}
