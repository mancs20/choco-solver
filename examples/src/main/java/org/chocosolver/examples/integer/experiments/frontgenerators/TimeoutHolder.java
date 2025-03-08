package org.chocosolver.examples.integer.experiments.frontgenerators;

import org.chocosolver.solver.Solver;

public interface TimeoutHolder {

    class TimeStorage {
        public static long lastUpdateTimeNano = System.nanoTime();
    }

    default float updateSolverTimeoutCurrentTime(Solver solver, float timeout){
        long currentTimeNano = System.nanoTime();
        float elapsedTime = (float) (currentTimeNano - TimeStorage.lastUpdateTimeNano) / 1_000_000_000;
        TimeStorage.lastUpdateTimeNano = currentTimeNano;
        timeout -= elapsedTime;
        solver.limitTime(timeout + "s");
        return timeout;
    }
}
