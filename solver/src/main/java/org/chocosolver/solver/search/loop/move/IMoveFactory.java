/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.loop.move;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.solver.ISelf;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.ICounter;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.loop.lns.neighbors.INeighbor;
import org.chocosolver.solver.search.restart.*;
import org.chocosolver.util.criteria.LongCriterion;

/**
 * Interface to define how to explore the search space from a macro perspective
 * (DFS, LDS, LNS, etc.)
 *
 * @author Charles Prud'Homme
 * @author Jean-Guillaume Fages
 */
public interface IMoveFactory extends ISelf<Solver> {

    /**
     * Depth-First Search algorithm with binary decisions
     */
    default void setDFS() {
        ref().setMove(new MoveBinaryDFS(ref().getSearch()));
    }

    /**
     * Limited Discrepancy Search[1] algorithms with binary decisions
     * <p>
     * [1]:W.D. Harvey and M.L.Ginsberg, Limited Discrepancy Search, IJCAI-95.
     *
     * @param discrepancy the maximum discrepancy
     */
    default void setLDS(int discrepancy) {
        IEnvironment env = ref().getEnvironment();
        ref().setMove(new MoveBinaryLDS(ref().getSearch(), discrepancy, env));
    }

    /**
     * Depth-bounded Discrepancy Search[1] algorithms with binary decisions
     * <p>
     * [1]:T. Walsh, Depth-bounded Discrepancy Search, IJCAI-97.
     *
     * @param discrepancy the maximum discrepancy
     */
    default void setDDS(int discrepancy) {
        IEnvironment env = ref().getEnvironment();
        ref().setMove(new MoveBinaryDDS(ref().getSearch(), discrepancy, env));
    }

    /**
     * Creates a move object based on:
     * Hybrid Best-First Search[1] algorithms with binary decisions.
     * <p>
     * [1]:D. Allouche, S. de Givry, G. Katsirelos, T. Schiex, M. Zytnicki,
     * Anytime Hybrid Best-First Search with Tree Decomposition for Weighted CSP, CP-2015.
     *
     * @param a lower bound to limit the rate of redundantly propagated decisions
     * @param b upper bound to limit the rate of redundantly propagated decisions.
     * @param N backtrack limit for each DFS try, should be large enough to limit redundancy
     */
    default void setHBFS(double a, double b, long N) {
        ref().setMove(new MoveBinaryHBFS(ref().getModel(), ref().getSearch(), a, b, N));
    }

    /**
     * Defines a restart policy.
     * Every time the <code>restartCriterion</code> is met, a restart is done, the new restart limit is updated
     * thanks to <code>restartStrategy</code>.
     * There will be at most <code>restartsLimit</code> restarts.
     *
     * @param restartCriterion the restart criterion, that is, the condition which triggers a restart
     * @param restartStrategy  the way restart limit (evaluated in <code>restartCriterion</code>) is updated, that is, computes the next limit
     * @param restartsLimit    number of allowed restarts
     */
    default void setRestarts(LongCriterion restartCriterion, ICutoff restartStrategy, int restartsLimit) {
        ref().setRestarts(restartCriterion, restartStrategy, restartsLimit, true);
    }

    /**
     * Defines a restart policy.
     * Every time the <code>restartCriterion</code> is met, a restart is done, the new restart limit is updated
     * thanks to <code>restartStrategy</code>.
     * There will be at most <code>restartsLimit</code> restarts.
     *
     * @param restartCriterion      the restart criterion, that is, the condition which triggers a restart
     * @param restartStrategy       the way restart limit (evaluated in <code>restartCriterion</code>) is updated, that is, computes the next limit
     * @param restartsLimit         number of allowed restarts
     * @param resetCutoffOnSolution reset cutoff sequence on solutions
     */
    default void setRestarts(LongCriterion restartCriterion, ICutoff restartStrategy, int restartsLimit,
                             boolean resetCutoffOnSolution) {
        ref().addRestarter(new Restarter(restartStrategy, restartCriterion, restartsLimit, resetCutoffOnSolution));
    }

    /**
     * Branch a luby restart strategy to the model.
     * The restartStrategyLimit is a {@link ICounter} to observe.
     * Once this counter reaches a limit, restart occurs and the next cutoff is computed.
     * <p>
     * At step <i>n</i>, the next cutoff is computed with following a Luby function.
     *
     * @param scaleFactor          scale factor
     * @param restartStrategyLimit restart trigger
     * @param restartLimit         restart limits (limit of number of restarts)
     */
    default void setLubyRestart(long scaleFactor, ICounter restartStrategyLimit, int restartLimit) {
        ref().setRestarts(restartStrategyLimit, new LubyCutoff(scaleFactor), restartLimit);
    }

    /**
     * Build a geometrical restart strategy.
     * The restartStrategyLimit is a {@link ICounter} to observe.
     * Once this counter reaches a limit, restart occurs and the next cutoff is computed.
     * <p>
     * At step <i>n</i>, the next cutoff is computed with the following function : <i>b*g^n</i>.
     *
     * @param base                 the initial limit
     * @param geometricalFactor    geometrical factor
     * @param restartStrategyLimit restart trigger
     * @param restartLimit         restart limits (limit of number of restarts)
     * @implNote Some counter may require an argument, as limit.
     * This declared limit will be ignored and replaced by <i>base</i> on creation of this.
     */
    default void setGeometricalRestart(long base, double geometricalFactor,
                                       ICounter restartStrategyLimit, int restartLimit) {
        ref().setRestarts(restartStrategyLimit, new GeometricalCutoff(base, geometricalFactor), restartLimit);
    }

    /**
     * Build a linear restart strategy.
     * The restartStrategyLimit is a {@link ICounter} to observe.
     * Once this counter reaches a limit, restart occurs and the next cutoff is computed.
     * <p>
     * At step <i>n</i>, the next cutoff is computed with the following function : <i>s^n</i>.
     *
     * @param scaleFactor          scale factor
     * @param restartStrategyLimit restart trigger
     * @param restartLimit         restart limits (limit of number of restarts)
     * @implNote Some counter may require an argument, as limit.
     * This declared limit will be ignored and replaced by <i>base</i> on creation of this.
     */
    default void setLinearRestart(long scaleFactor, ICounter restartStrategyLimit, int restartLimit) {
        ref().setRestarts(restartStrategyLimit, new LinearCutoff(scaleFactor), restartLimit);
    }

    /**
     * Build a linear restart strategy.
     * The restartStrategyLimit is a {@link ICounter} to observe.
     * Once this counter reaches a limit, restart occurs and the next cutoff is computed.
     * <p>
     * At step <i>n</i>, the next cutoff is computed with the following function : <i>s</i>.
     *
     * @param scaleFactor          scale factor
     * @param restartStrategyLimit restart trigger
     * @param restartLimit         restart limits (limit of number of restarts)
     * @implNote Some counter may require an argument, as limit.
     * This declared limit will be ignored and replaced by <i>base</i> on creation of this.
     */
    default void setConstantRestart(long scaleFactor, ICounter restartStrategyLimit, int restartLimit) {
        ref().setRestarts(restartStrategyLimit, new MonotonicCutoff(scaleFactor), restartLimit);
    }

    /**
     * Creates restart strategy that restarts every time a solution is found.
     */
    default void setRestartOnSolutions() {
        ref().addRestarter(new Restarter(
                new MonotonicCutoff(1),
                new SolutionCounter(ref().getModel(), 1),
                Integer.MAX_VALUE, true));
    }

    /**
     * Creates a Move object based on Large Neighborhood Search.
     * It encapsulates the current move within a LNS move.
     * Anytime a solution is encountered, it is recorded and serves as a basis for the <code>INeighbor</code>.
     * The <code>INeighbor</code> creates a <i>fragment</i>: selects variables to freeze/unfreeze wrt the last solution found.
     * If a fragment cannot be extended to a solution, a new one is selected by restarting the search.
     * If a fragment induces a search space which a too big to be entirely evaluated, restarting the search can be forced
     * using the <code>restartCriterion</code>. A fast restart strategy is often a good choice.
     *
     * @param neighbor       the neighbor for the LNS
     * @param restartCounter the (fast) restart counter. Initial limit gives the frequency.
     * @param bootstrap      if a solution is known, it can be given to LNS
     */
    default void setLNS(INeighbor neighbor, ICounter restartCounter, Solution bootstrap) {
        MoveLNS lns = new MoveLNS(ref().getMove(), neighbor, restartCounter);
        ref().setMove(lns);
        if (bootstrap != null) {
            lns.loadFromSolution(bootstrap, ref());
        }
    }


    /**
     * Creates a Move object based on Large Neighborhood Search.
     * It encapsulates the current move within a LNS move.
     * Anytime a solution is encountered, it is recorded and serves as a basis for the <code>INeighbor</code>.
     * The <code>INeighbor</code> creates a <i>fragment</i>: selects variables to freeze/unfreeze wrt the last solution found.
     * If a fragment cannot be extended to a solution, a new one is selected by restarting the search.
     *
     * @param neighbor  the neighbor for the LNS
     * @param bootstrap if a solution is known, it can be given to LNS
     * @see #setLNS(INeighbor, ICounter)
     */
    default void setLNS(INeighbor neighbor, Solution bootstrap) {
        setLNS(neighbor, ICounter.Impl.None, bootstrap);
    }

    /**
     * Creates a Move object based on Large Neighborhood Search.
     * It encapsulates the current move within a LNS move.
     * Anytime a solution is encountered, it is recorded and serves as a basis for the <code>INeighbor</code>.
     * The <code>INeighbor</code> creates a <i>fragment</i>: selects variables to freeze/unfreeze wrt the last solution found.
     * If a fragment cannot be extended to a solution, a new one is selected by restarting the search.
     * If a fragment induces a search space which a too big to be entirely evaluated, restarting the search can be forced
     * using the <code>restartCriterion</code>. A fast restart strategy is often a good choice.
     *
     * @param neighbor       the neighbor for the LNS
     * @param restartCounter the (fast) restart counter. Initial limit gives the frequency.
     */
    default void setLNS(INeighbor neighbor, ICounter restartCounter) {
        setLNS(neighbor, restartCounter, null);
    }


    /**
     * Creates a Move object based on Large Neighborhood Search.
     * It encapsulates the current move within a LNS move.
     * Anytime a solution is encountered, it is recorded and serves as a basis for the <code>INeighbor</code>.
     * The <code>INeighbor</code> creates a <i>fragment</i>: selects variables to freeze/unfreeze wrt the last solution found.
     * If a fragment cannot be extended to a solution, a new one is selected by restarting the search.
     *
     * @param neighbor the neighbor for the LNS
     * @see #setLNS(INeighbor, ICounter)
     */
    default void setLNS(INeighbor neighbor) {
        setLNS(neighbor, ICounter.Impl.None, null);
    }
}
