package org.chocosolver.examples.integer.experiments.frontgenerators;

import org.chocosolver.solver.Solver;

public interface TimeoutHolder {

    default float updateSolverTimeoutCurrentTime(Solver solver, float timeout, long startTime){
        long currentTime = System.nanoTime();
        float elapsedTime = (currentTime - startTime) / 1_000_000_000f;
        float newTimeout = timeout - elapsedTime;

        if (newTimeout > 0) {
            solver.limitTime(newTimeout + "s");
        } else {
            newTimeout = 0;
        }
        return newTimeout;
    }
}
