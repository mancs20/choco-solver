package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

public interface ProblemPermutationFlowshopWithoutTask {
    default ModelObjectivesVariables implementFlowShopPermutation(String modelName, int[][] jobTaskDuration,
                                                                  int[] dueDates){
        Model model = new Model(modelName);
        int nJobs = dueDates.length;
        int nTasks = jobTaskDuration[0].length;
        int minDuration = 0;
        int allJobsDuration = 0;
        int lbJobTardiness = 0;
        int ubJobTardiness = 0;
        for (int j = 0; j < nJobs; j++) {
            int jobDuration = 0;
            for (int t = 0; t < nTasks; t++) {
                jobDuration += jobTaskDuration[j][t];
            }
            allJobsDuration += jobDuration;
            if (jobDuration > minDuration) {
                minDuration = jobDuration;
            }
            lbJobTardiness += (jobDuration - dueDates[j]);
        }
        for (int j = 0; j < nJobs; j++) {
            ubJobTardiness += (allJobsDuration - dueDates[j]);
        }

        // Variables
        //--------------------------------------------------------------------------------------------------------------
        IntVar[][] jobTaskStart = model.intVarMatrix("jobTaskStart", nJobs, jobTaskDuration[0].length,
                0, allJobsDuration);
        IntVar[] jobsOrder;

        // Constraints
        //--------------------------------------------------------------------------------------------------------------
        // todo try with cumulative constraint

        // The first job task can start no earlier than time step 0.
        for (int j=0; j<nJobs; j++) {
            model.arithm(jobTaskStart[j][0], ">=", 0).post();
        }

        // Each job task must complete before the next.
        for (int j=0; j<nJobs; j++) {
            for (int t=0; t<nTasks - 1; t++) {
                model.arithm(jobTaskStart[j][t+1], "-", jobTaskStart[j][t], ">=", jobTaskDuration[j][t]).post();
            }
        }

        // Flow shop permutation established order
        jobsOrder = model.intVarArray("jobsOrder", nJobs, 0, nJobs - 1);
        model.allDifferent(jobsOrder).post();

        // The order for the job tasks is the same for all the machines
        for (int j=0; j<nJobs; j++) {
            for (int t=0; t<nTasks; t++) {
                model.arithm(jobTaskStart[jobsOrder[j+1].getValue()][t], "-",
                        jobTaskStart[jobsOrder[j].getValue()][t], ">=",
                        jobTaskDuration[jobsOrder[j].getValue()][t]).post();
            }
        }


        // Objectives
        //--------------------------------------------------------------------------------------------------------------
        IntVar tEnd = model.intVar(minDuration, allJobsDuration);
        IntVar sumTardiness = model.intVar(lbJobTardiness, ubJobTardiness);

        // The finishing time must be no earlier than the finishing time of the last task.
        model.arithm(tEnd, "-", jobTaskStart[jobsOrder[nJobs - 1].getValue()][nTasks - 1], "=",
                jobTaskDuration[jobsOrder[nJobs - 1].getValue()][nTasks - 1]).post();

        // The tardiness of each job is the difference between the completion time and the due date.
        IntVar[] lastJobsTaskStartSumTardiness = new IntVar[nJobs + 1];
        for (int j=0; j<nJobs; j++) {
            lastJobsTaskStartSumTardiness[j] = jobTaskStart[j][nTasks - 1];
        }
        lastJobsTaskStartSumTardiness[nJobs] = sumTardiness;
        int[] weights = new int[nJobs + 1];
        for (int j=0; j<nJobs; j++) {
            weights[j] = 1;
        }
        weights[nJobs] = -1;
        int sumDueDatesMinusSumJobDuration = 0;
        for (int j=0; j<nJobs; j++) {
            sumDueDatesMinusSumJobDuration += dueDates[j] - jobTaskDuration[j][nTasks - 1];
        }
        model.scalar(lastJobsTaskStartSumTardiness, weights, "=", sumDueDatesMinusSumJobDuration).post();

        return new ModelObjectivesVariables(model, new IntVar[]{tEnd, sumTardiness}, jobTaskStart, false);
    }
}
