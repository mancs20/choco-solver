package org.chocosolver.solver.objective.mocoframework.component.findsolution;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.IObjectiveManager;
import org.chocosolver.solver.objective.MaxIntObjManagerWithObjsLB;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.objective.mocoframework.util.CounterSnapshot;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.search.limits.ICounter;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.ArrayList;
import java.util.List;

import static org.chocosolver.solver.search.SearchState.STOPPED;

public class GIAOptSumFindSolution extends AbstractFindSolutionStrategy {
    private int solverCallsCount;
    private Constraint[] objectiveConstraints;

    public GIAOptSumFindSolution(SolutionFinder solutionFinder) {
        super(solutionFinder);
        solverCallsCount = 0;
    }

    @Override
    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        // Implement the logic to find a non-dominated solution using the GIA approach with sum of objectives as the optimization criterion.
        // This is a placeholder implementation and should be replaced with the actual logic.
        if (objectiveConstraints == null) {
            objectiveConstraints = new Constraint[objectives.length];
        }
        List<CounterSnapshot> snapshots = new ArrayList<>();
        for (Criterion c : stop) {
            if (c instanceof ICounter) {
                snapshots.add(new CounterSnapshot((ICounter) c));
            } else {
                model.getSolver().addStopCriterion(c);
            }
        }

        long pre = model.getSolver().getSolutionCount();
        // recycle a solution from the pool of dominated solutions
        Solution sol = archive.borrowDominatedSolutionFromPool();
        params.getParetoMaximizer().setEnabled(true);
        optimizeObjectiveFunction(model, sol, objectives, params.getParetoMaximizer());

        boolean foundSolution = model.getSolver().getSolutionCount() > pre;

        if (model.getSolver().getSearchState() == STOPPED) {
            params.setExhaustive(false);
        }

        for (CounterSnapshot snapshot : snapshots) {
            snapshot.adjustUsedSoFar();
        }
        params.getRecorderList().add(model.getSolver().getMeasures().toString());
        solverCallsCount++;
        model.getSolver().reset(); // model.getSolver().getSearch()
        model.getSolver().getMeasures().setRestartCount(solverCallsCount);
        for (CounterSnapshot snapshot : snapshots) {
            snapshot.reapplyToSolver(model.getSolver());
        }

        if (foundSolution) {
            return sol;
        }
        archive.returnDominatedSolutionToPool(sol);
        return null;
    }

    private void optimizeObjectiveFunction(Model model, Solution sol, IntVar[] objectives, ParetoMaximizer paretoConstraint) {
        boolean foundSolution = false;
        int[] objLBs = new int[objectives.length];
        IObjectiveManager<?> om = model.getSolver().getObjectiveManager();
        while (model.getSolver().solve()) {
            sol.record();
            for (int i = 0; i < objLBs.length; i++) {
                objLBs[i] = sol.getIntVal(objectives[i]);
            }
            if (!foundSolution) {
                foundSolution = true;
                paretoConstraint.setEnabled(false);
                ((MaxIntObjManagerWithObjsLB) om).setObjectivesLowerBound(objLBs);
            }
        }
    }
}
