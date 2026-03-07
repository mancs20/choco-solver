package org.chocosolver.solver.objective.mocoframework.util;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.UpdatablePropagator;
import org.chocosolver.solver.constraints.nary.lex.PropLexInt;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.search.limits.ICounter;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.ArrayList;
import java.util.List;

import static org.chocosolver.solver.search.SearchState.STOPPED;

public class SolutionFinder {
    private int solverCallsCount;
    private boolean disableParetoMaximizerAfterFirstSolution = false;
    UpdatablePropagator<int[]> plint = null;
    private int[] bestLexFound = null;
    private IntVar[] mobj = null;

    public SolutionFinder() {
        solverCallsCount = 0;
    }

    public Solution find(Model model, ParetoArchive archive, IntVar[] objectives, Region region, StrategyParams params, Criterion... stop) {
        if (model.getSolver().isStopCriterionMet()) {
            return null;
        }

        List<CounterSnapshot> snapshots = new ArrayList<>();
        if (params.isOptimization() || params.isLexicographicOptimization()) {
            for (Criterion c : stop) {
                if (c instanceof ICounter) {
                    snapshots.add(new CounterSnapshot((ICounter) c));
                } else {
                    model.getSolver().addStopCriterion(c);
                }
            }
        }

        disableParetoMaximizerAfterFirstSolution = params.isDisableParetoMaximizerAfterFirstSolution();

        region.postConstraints();
        boolean addIntermediateSolutions = params.isAddIntermediateSolutions();

        long pre = model.getSolver().getSolutionCount();
        // recycle a solution from the pool of dominated solutions
        Solution sol = null;
        if (!addIntermediateSolutions) {
            sol = archive.borrowDominatedSolutionFromPool();
        }
        if (params.isOptimization()) {
            sol = optimizeObjectiveFunction(model, sol, archive, addIntermediateSolutions);
        } else if (params.isLexicographicOptimization()) {
            sol = lexicographicOptimization(model, sol, archive, addIntermediateSolutions, objectives, params);
        } else {
            if (model.getSolver().solve()) {
                assert sol != null;
                sol.record();
            }
        }
        boolean foundSolution = model.getSolver().getSolutionCount() > pre;

        if (model.getSolver().getSearchState() == STOPPED) {
            params.setExhaustive(false);
        }

        if (params.isOptimization() || params.isLexicographicOptimization()) {
            for (CounterSnapshot snapshot : snapshots) {
                snapshot.adjustUsedSoFar();
            }
            params.getRecorderList().add(model.getSolver().getMeasures().toString());
            solverCallsCount++;
            model.getSolver().reset();
            model.getSolver().getMeasures().setRestartCount(solverCallsCount);
            for (CounterSnapshot snapshot : snapshots) {
                snapshot.reapplyToSolver(model.getSolver());
            }
        }

        region.unpostConstraints(model);
        if (foundSolution) return sol;
        if (sol != null) {
            archive.returnDominatedSolutionToPool(sol);
        }
        return null;
    }

    private Solution optimizeObjectiveFunction(Model model, Solution sol, ParetoArchive archive, boolean addIntermediateSolutions) {
        while (model.getSolver().solve()) {
            if (addIntermediateSolutions) {
                Solution added = archive.addIntermediateSolutions();
                if (added != null) sol = added;
            } else {
                sol.record();
            }
        }
        return sol;
    }

    private Solution lexicographicOptimization(Model model, Solution sol, ParetoArchive archive,
                                               boolean addIntermediateSolutions, IntVar[] objectives, StrategyParams params) {

        boolean paretoMaximizerDisabled = false;
        // todo put an order in the objectives and order them accordingly mobj[i] = model.neg(objectives[order[i]])
        // Lexicographic optimization
        // set best lex array to the lower bound and deactivate the propagator until a first solution is obtained
        if (bestLexFound == null) {
            bestLexFound = new int[objectives.length];
        } else {
            for (int vIdx = 0; vIdx < objectives.length; vIdx++) {
                bestLexFound[vIdx] = -objectives[vIdx].getLB();
            }
            plint.update(bestLexFound, false);
            ((PropLexInt) plint).setEnabled(false);
        }
        // 1. copy objective variables and transform it if necessary
        if (mobj == null) {
            mobj = new IntVar[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                //todo delete the commented line once tested
                mobj[i] = model.neg(objectives[i]);
//                mobj[i] = model.neg(objectives[order[i]]);
            }
        }
        // 2. try to find a first solution
        while (model.getSolver().solve()) {
            if (addIntermediateSolutions) {
                Solution added = archive.addIntermediateSolutions();
                if (added != null) sol = added;
            } else {
                sol.record();
            }
            // todo add a flag to params to indicate verbose and then use recorder. It is to show at every second
            //  the current archive
//                recorder.onNewSolution(sol, objectives);
            // 3. extract values of each objective
            for (int vIdx = 0; vIdx < objectives.length; vIdx++) {
                bestLexFound[vIdx] = -objectives[vIdx].getValue();
            }

            if (!paretoMaximizerDisabled && disableParetoMaximizerAfterFirstSolution && bestLexFound[0] < params.objectiveValueToDisableParetoMaximizer) {
                params.getParetoMaximizer().setEnabled(false);
                paretoMaximizerDisabled = true;
            }

            // 4. either update the constraint, or declare it if first solution
            if (plint != null) {
                // enabled the propagator for lexicographic optimization if not already done
                ((PropLexInt) plint).setEnabled(true);
                plint.update(bestLexFound, true);
            } else {
                plint = new PropLexInt(mobj, bestLexFound, true, true);
                //noinspection unchecked
                Constraint clint = new Constraint("lex objectives", (Propagator<IntVar>) plint);
                clint.post();
            }
        }
        if (paretoMaximizerDisabled) {
            params.getParetoMaximizer().setEnabled(true);
        }
        return sol;
    }
}
