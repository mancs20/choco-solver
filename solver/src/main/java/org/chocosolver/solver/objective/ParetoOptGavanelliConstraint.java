package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
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
        this.setObjective(objectives);
        this.timeout = timeout;
        solveCallsCount = 0;
        exhaustive = false;
        startTime = System.nanoTime();
    }

    private void setObjective(IntVar[] objectives) {
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

        while (!exhaustive && !stopCriterionReached) {
            float remainingTimeout = updateSolverTimeoutCurrentTime(model.getSolver(), timeout, startTime);
            if (remainingTimeout <= 0) {
                stopCriterionReached = true;
            } else {
                Solution solution = new Solution(model);
                while (model.getSolver().solve()) {
                    solution.record();
                    pareto.onSolution();
                }

                if (solution.exists()) {
                    recorderList.add(model.getSolver().getMeasures().toString());
                } else {
                    exhaustive = true;
                }
                if (!model.getSolver().isStopCriterionMet()){
                    model.getSolver().reset();
                    solveCallsCount++;
                    model.getSolver().getMeasures().setRestartCount(solveCallsCount);
                }else {
                    stopCriterionReached = true;
                }
            }
        }
        this.solutions = pareto.getParetoFront();
    }
}
