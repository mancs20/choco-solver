package org.chocosolver.examples.integer.experiments.frontgenerators;

import org.chocosolver.solver.Solver;

public interface TimeoutHolder {

    default float updateSolverTimeoutCurrentTime(Solver solver, float timeout, long startTimeNano){
        long currentTimeNano = System.nanoTime();
        float elapsedTime = (float) (currentTimeNano - startTimeNano) / 1_000_000_000;
        timeout = timeout - elapsedTime;
        solver.limitTime(timeout + "s");
        return timeout;
    }
}
