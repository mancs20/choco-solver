package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TaillardDznFileReader extends BenchamarkReader implements ProblemPermutationFlowshop {

    public TaillardDznFileReader(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        String filePath = config.getInstancePath();
        int nJobs = 0;
        int nMachines = 0;
        int[] dueDates = null;
        int [][] jobTaskDuration = null;

        try(BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean reading2DArray = false;
            int jobIndex = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!reading2DArray) {
                    if (line.startsWith("n_jobs")) {
                        // get the number after the '=' sign and before the ';' sign
                        nJobs = Integer.parseInt(line.split(" = ")[1].split(";")[0]);
                    } else if (line.startsWith("n_machines")) {
                        nMachines = Integer.parseInt(line.split(" = ")[1].split(";")[0]);
                    } else if (line.startsWith("job_task_duration")) {
                        reading2DArray = true;
                        jobTaskDuration = new int[nJobs][nMachines];
                    } else if (line.startsWith("due_date_jobs")) {
                        String[] dueDatesArray = line.split(" = ")[1].split("\\[")[1].split("\\]")[0].split(", ");
                        if (dueDatesArray.length != nJobs) {
                            System.err.println("The number of due dates does not match the number of jobs");
                            System.exit(1);
                        } else {
                            dueDates = new int[dueDatesArray.length];
                            for (int i = 0; i < dueDatesArray.length; i++) {
                                dueDates[i] = Integer.parseInt(dueDatesArray[i]);
                            }
                        }
                    }
                } else {
                    if (line.startsWith("]")) {
                        reading2DArray = false;
                    } else {
                        if (line.endsWith(",")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        String[] durations = line.split(", ");
                        for (int i = 0; i < durations.length; i++) {
                            jobTaskDuration[jobIndex][i] = Integer.parseInt(durations[i]);
                        }
                        jobIndex++;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return implementFlowShopPermutation(getModelName(index), jobTaskDuration, dueDates);
    }
}
