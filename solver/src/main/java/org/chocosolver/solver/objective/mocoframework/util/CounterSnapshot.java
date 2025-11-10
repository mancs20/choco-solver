package org.chocosolver.solver.objective.mocoframework.util;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.ICounter;

public class CounterSnapshot {
    private final ICounter counter;
    private long usedSoFar;
    private final long originalLimit;

    public CounterSnapshot(ICounter counter) {
        this.counter = counter;
        this.usedSoFar = counter.currentValue();
        this.originalLimit = counter.getLimitValue();
    }

    public void adjustUsedSoFar() {
        this.usedSoFar = counter.currentValue();
    }

    public void reapplyToSolver(Solver solver) {
        long remaining = Math.max(0, originalLimit - usedSoFar);
        counter.overrideLimit(remaining);
        solver.addStopCriterion(counter);
    }
}
