package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

public abstract class AbstractFindSolutionStrategy implements FindNonDominatedSolutionStrategy {
    protected final SolutionFinder solutionFinder;

    protected AbstractFindSolutionStrategy(SolutionFinder solutionFinder) {
        this.solutionFinder = solutionFinder;
    }

    @Override
    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        return solutionFinder.find(model, archive, objectives, region, params, stop);
    }
}
