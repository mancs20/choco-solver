package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.moexperiments.TimeoutHolder;

import java.util.stream.Stream;

public class ParetoOptGavanelliConstraint extends ParetoAbstract implements TimeoutHolder {

    private final Model model;
    private final IntVar[] objectives;
    private final int timeout;
    protected int solveCallsCount;
    private boolean stopCriterionReached;
    private final long startTime;


    public ParetoOptGavanelliConstraint(Model model, IntVar[] objectives, boolean maximize, int timeout) {
        this.model = model;
        this.objectives = Stream.of(objectives).map(o -> maximize ? o : model.neg(o)).toArray(IntVar[]::new);
        this.setObjective();
        this.timeout = timeout;
        solveCallsCount = 0;
        exhaustive = false;
        startTime = System.nanoTime();
    }

    private void setObjective() {
        int ubObj = 0;
        int lbObj = 0;
        for (IntVar obj: objectives) {
            ubObj += obj.getUB();
            lbObj += obj.getLB();
        }
        IntVar objective = model.intVar("objectivesSum", lbObj, ubObj);
        model.sum(objectives, "=", objective).post();
        model.setObjective(true, objective);
    }


    public void findFront() {
        ParetoMaximizer pareto = new ParetoMaximizer(objectives);
        Constraint c = new Constraint("PARETO", pareto);
        c.post();
        long timeFindingFeasible = 0L;
        long timeProvingOptimum = 0L;

        while (!exhaustive && !stopCriterionReached) {
            float remainingTimeout = updateSolverTimeoutCurrentTime(model.getSolver(), timeout, startTime);
            if (remainingTimeout <= 0) {
                stopCriterionReached = true;
            } else {
                exhaustive = true;
                long __t0_findinSol = System.nanoTime();
                while (model.getSolver().solve()) {
                    timeFindingFeasible += System.nanoTime() - __t0_findinSol;
                    exhaustive = false;
                    pareto.onSolution();
                    __t0_findinSol = System.nanoTime();
                }
                timeProvingOptimum += System.nanoTime() - __t0_findinSol;
                recorderList.add(model.getSolver().getMeasures().toString());
                if (!model.getSolver().isStopCriterionMet()) {
                    if (!exhaustive) {
                        model.getSolver().reset();
                        solveCallsCount++;
                        model.getSolver().getMeasures().setRestartCount(solveCallsCount);
                    }
                } else {
                    stopCriterionReached = true;
                    exhaustive = false;
                }
            }
        }
        this.solutions = pareto.getParetoFront();
        long totalTimeAprox = timeFindingFeasible + timeProvingOptimum;
        System.out.println("Time finding feasible solutions (s): " + timeFindingFeasible/1_000_000_000.0);
        System.out.println("Time proving optimality (s): " + timeProvingOptimum/1_000_000_000.0);
        System.out.println("Number of time a the same non-dominated point in the objective space was reached in the global constraint: " + pareto.sameSolutionReached);
        System.out.println("Time adding solution to Archive / time finding tightest point (Pareto global) / aprox solution" +
                "(s): " + pareto.timeAddingSolutionToArchiveRemovingDominates /1_000_000_000.0 + "/" +
                pareto.timeFindingTightestPoint /1_000_000_000.0 + "/" + totalTimeAprox/1_000_000_000.0);


    }
}
