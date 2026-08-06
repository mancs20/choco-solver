/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.List;
import java.util.stream.Stream;

/**
 * Base class for algorithms computing the Pareto front.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public abstract class ParetoFrontFinder {

    /**
     * Computes a Pareto front.
     *
     * @param solver solver used to perform the search
     * @param objectives objective variables
     * @param maximize whether the objectives must be maximized or minimized
     * @param stop optional criteria stopping the search
     * @return the Pareto solutions found
     */
    public abstract List<Solution> findParetoFront(
            Solver solver,
            IntVar[] objectives,
            boolean maximize,
            Criterion... stop
    );

    /**
     * Turns minimization objectives into their opposite views so that concrete
     * algorithms can uniformly maximize every objective.
     */
    protected IntVar[] normalizeObjectives(Solver solver, IntVar[] objectives, boolean maximize) {
        return Stream.of(objectives)
                .map(objective -> maximize ? objective : solver.getModel().neg(objective))
                .toArray(IntVar[]::new);
    }
}
