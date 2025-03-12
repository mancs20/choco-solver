package org.chocosolver.examples.integer.moexperiments;

import org.chocosolver.examples.integer.experiments.frontgenerators.TimeoutHolder;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.testng.annotations.Test;

import static java.lang.Thread.sleep;

public class TimeoutHolderTest implements TimeoutHolder {
    Model model = new Model();
    Solver solver = model.getSolver();

    @Test(groups="1m", timeOut=60000)
    public void testUpdateSolverTimeoutCurrentTimeSolvingInTime() {
        float timeout = 10;
        float newTimeout;
        long startTime = System.nanoTime();
        // simulate solving
        int[] resolutionTimes = {1, 6, 2};
        int[] timeLimits = {9, 3, 1};
        for (int i = 0; i < resolutionTimes.length; i++) {
            try {
                sleep(resolutionTimes[i] * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            newTimeout = updateSolverTimeoutCurrentTime(solver, timeout, startTime);
            System.out.println("newTimeout = " + newTimeout + " timeLimits[i] = " + timeLimits[i]);
            assert Math.ceil(newTimeout) == timeLimits[i];
        }
    }

    @Test(groups="1m", timeOut=60000)
    public void testUpdateSolverTimeoutCurrentTimeNoNegativeTimeout() {
        long startTime = System.nanoTime();
        float timeout = 3;
        // simulate solving
        int[] resolutionTimes = {1, 4};
        float newTimeout = -1;
        for (int i = 0; i < resolutionTimes.length; i++) {
            try {
                sleep(resolutionTimes[i] * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            newTimeout = updateSolverTimeoutCurrentTime(solver, timeout, startTime);
            System.out.println("newTimeout = " + newTimeout);
        }
        assert newTimeout >= 0;
    }
}
