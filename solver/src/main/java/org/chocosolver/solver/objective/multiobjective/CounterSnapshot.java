/*
 * This file is part of choco-solver, http://choco-solver.org/
 * Copyright (c) 1999, IMT Atlantique.
 * SPDX-License-Identifier: BSD-3-Clause.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.objective.multiobjective;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.ICounter;

/**
 * Class to save the current metrics of the solver before doing a reset and apply them again after the reset
 *
 * @author Manuel Combarro Simón (combarro87@gmail.com)
 */
final class CounterSnapshot {
    private final ICounter counter;
    private long remainingLimit;

    public CounterSnapshot(ICounter counter) {
        this.counter = counter;
        this.remainingLimit = counter.getLimitValue();
    }

    public void updateRemainingLimit() {
        this.remainingLimit = Math.max(0, remainingLimit - counter.currentValue());
    }

    public void reapplyToSolver(Solver solver) {
        counter.overrideLimit(remainingLimit);
        solver.addStopCriterion(counter);
    }
}
