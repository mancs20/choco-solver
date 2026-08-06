/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.util.List;

/**
 * Computes a Pareto front by posting the Pareto global constraint and
 * enumerating solutions until infeasibility.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public final class MobabParetoFrontFinder extends ParetoFrontFinder {

    @Override
    public List<Solution> findParetoFront(
            Solver solver,
            IntVar[] objectives,
            boolean maximize,
            Criterion... stop
    ) {
        solver.addStopCriterion(stop);
        solver.getModel().clearObjective();
        ParetoMaximizer pareto = new ParetoMaximizer(
                normalizeObjectives(solver, objectives, maximize)
        );
        Constraint constraint = new Constraint("PARETO", pareto);
        constraint.post();
        while (solver.solve()) {
            pareto.onSolution();
        }
        solver.removeStopCriterion(stop);
        solver.getModel().unpost(constraint);
        return pareto.getParetoFront();
    }
}
