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
 * <p>
 * Based on "An Algorithm for Multi-Criteria Optimization in CSPs", M. Gavanelli (ECAI 2002).
 * <p>
 * See also <a href="https://doi.org/10.1007/978-3-642-40627-0_46">Multi-Objective Large Neighborhood Search</a>,
 * P. Schaus and R. Hartert (CP 2013).
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public final class MobabParetoFrontFinder extends ParetoFrontFinder {

    @Override
    protected List<Solution> findParetoFront(
            Solver solver,
            IntVar[] objectives,
            Criterion... stop
    ) {
        solver.addStopCriterion(stop);
        solver.getModel().clearObjective();
        ParetoMaximizer pareto = new ParetoMaximizer(objectives);
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
