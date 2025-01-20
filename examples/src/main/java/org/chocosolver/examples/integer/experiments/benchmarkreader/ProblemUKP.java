package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;


public interface ProblemUKP {

    default ModelObjectivesVariables implementKnapsackModel(Model model, int[][] objectives, int[] weightsLHS, int rhs){
        // For each type, we create a variable for the number of occurrences 0 or 1 of each item
        IntVar[] occurrences = new IntVar[weightsLHS.length];
        for (int i = 0; i < weightsLHS.length; i++) {
            occurrences[i] = model.intVar(0, 1);
        }

        IntVar weightConstraintRHS = model.intVar(0, rhs);
        int numberOfObjectives = objectives.length;
        IntVar[] objectiveVars = new IntVar[numberOfObjectives];
        for (int i = 0; i < numberOfObjectives; i++) {
            int objectiveUB = Arrays.stream(objectives[i]).sum();
            objectiveVars[i] = model.intVar(0, objectiveUB);
            model.knapsack(occurrences, weightConstraintRHS, objectiveVars[i], weightsLHS, objectives[i]).post();
        }

        return new ModelObjectivesVariables(model, objectiveVars, occurrences, true);
    }

    default ModelObjectivesVariables implementKnapsackModel(String modelName, int[][] objectives, int[] weightsLHS, int rhs){
        Model model = new Model(modelName);
        // For each type, we create a variable for the number of occurrences 0 or 1 of each item
        IntVar[] occurrences = new IntVar[weightsLHS.length];
        for (int i = 0; i < weightsLHS.length; i++) {
            occurrences[i] = model.intVar(0, 1);
        }

        IntVar weightConstraintRHS = model.intVar(0, rhs);
        int numberOfObjectives = objectives.length;
        IntVar[] objectiveVars = new IntVar[numberOfObjectives];
        for (int i = 0; i < numberOfObjectives; i++) {
            int objectiveUB = Arrays.stream(objectives[i]).sum();
            objectiveVars[i] = model.intVar(0, objectiveUB);
            model.knapsack(occurrences, weightConstraintRHS, objectiveVars[i], weightsLHS, objectives[i]).post();
        }

        return new ModelObjectivesVariables(model, objectiveVars, occurrences, true);
    }
}
