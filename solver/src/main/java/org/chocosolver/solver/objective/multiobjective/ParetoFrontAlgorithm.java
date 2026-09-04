/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

/**
 * Built-in algorithms for computing Pareto fronts.
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
public enum ParetoFrontAlgorithm {

    /**
     * Multi-objective branch-and-bound using the Pareto global constraint.
     * <p>
     * Based on "An Algorithm for Multi-Criteria Optimization in CSPs", M. Gavanelli (ECAI 2002).
     * <p>
     * See also <a href="https://doi.org/10.1007/978-3-642-40627-0_46">Multi-Objective Large Neighborhood Search</a>,
     * P. Schaus and R. Hartert (CP 2013).
     */
    MOBAB {
        @Override
        public ParetoFrontFinder create() {
            return new MobabParetoFrontFinder();
        }
    },

    /**
     * Multi-objective algorithm robust for problems with 2 and 3 objectives. Combines ideas of SAUGMECON
     * (an epsilon-constraint method) with the Pareto global constraint.
     * <p>
     * Based on <a href="https://doi.org/10.4230/LIPIcs.CP.2026.14">Combining an ε-Constraint Method with the
     * Pareto Global Constraint</a>, M. Combarro Simón, P. Talbot and P. Bouvry (CP 2026).
     */
    PAUGMECON {
        @Override
        public ParetoFrontFinder create() {
            return new PaugmeconParetoFrontFinder();
        }
    };

    /**
     * @return a new finder implementing this algorithm
     */
    public abstract ParetoFrontFinder create();
}
