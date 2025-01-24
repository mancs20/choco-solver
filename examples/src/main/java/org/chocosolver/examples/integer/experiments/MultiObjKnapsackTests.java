package org.chocosolver.examples.integer.experiments;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;

public class MultiObjKnapsackTests {
    public static void main(String[] args) {
        ukpVoptLibK5050W06();
    }

    private static void ukpVoptLibK5050W06(){
        int[] nbItems = new int[50]; // number of items for each type
        for (int i = 0; i < 50; i++) {
            nbItems[i] = 1;
        }
        int[] weights = new int[]{62,300,670,372,535,162,985,952,562,417,2,817,554,9,128,619,934,805,100,334,149,668,
                647,164,709,421,838,659,152,563,134,60,374,317,478,419,807,220,511,986,928,82,841,608,874,72,458,969,912,314};
        int[] obj1Coeff = new int[]{684,45,678,805,478,549,302,485,788,623,652,583,682,641,320,258,49,402,79,123,759,563,328,
                531,400,985,889,121,130,329,618,113,224,451,512,761,577,273,753,217,881,714,889,114,391,100,471,44,751,234};
        int[] obj2Coeff = new int[]{168,547,584,513,978,417,914,225,226,65,58,519,708,429,117,213,37,69,693,734,507,279,163,
                8,321,177,978,473,712,274,665,392,778,336,18,673,58,495,460,725,285,902,859,143,171,167,732,581,926,269};

        int knapsackCapacity = 11674;

        Model model = new Model("MultiObjKnapsackVoptLibK5050W06");

        // For each type, we create a variable for the number of occurrences
        IntVar[] occurrences = new IntVar[nbItems.length];
        for (int i = 0; i < nbItems.length; i++) {
            occurrences[i] = model.intVar(0, 1);
        }

        // model variables
        IntVar weight = model.intVar(0, knapsackCapacity);
        int obj1UB = Arrays.stream(obj1Coeff).sum();
        int obj2UB = Arrays.stream(obj2Coeff).sum();
        IntVar obj1 = model.intVar(0, obj1UB);
        IntVar obj2 = model.intVar(0, obj2UB);
        IntVar[] objectives = new IntVar[]{obj1, obj2};

        // We add two knapsack constraints to the solver
        // Beware : call the post() method to save it
        model.knapsack(occurrences, weight, obj1, weights, obj1Coeff).post();
//        model.scalar(occurrences, obj2Coeff, "=", obj2).post(); // with this constraint instead of another knapsack, all the solutions are found doing reset, but takes too long
        model.knapsack(occurrences, weight, obj2, weights, obj2Coeff).post();
        float timeout = 3200;

        try {
            model.getSolver().testUkpVoptLibK5050W06(objectives, true, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void toiExample(){
        int[] nbItems = new int[]{1, 1, 1, 1}; // number of items for each type
        int[] weights = new int[]{1, 1, 1, 1}; // weight of an item for each type
        int[] obj1 = new int[]{4, 3, 2, 1}; // quantity of obj1 of the item
        int[] obj2 = new int[]{1, 2, 3, 4}; // quantity of obj2 of the item

        Model model = new Model("ParetoKnapsack");

        // For each type, we create a variable for the number of occurrences
        IntVar[] occurrences = new IntVar[nbItems.length];
        for (int i = 0; i < nbItems.length; i++) {
            occurrences[i] = model.intVar(0, nbItems[i]);
        }

        // Total weight of the solution.
        IntVar weight = model.intVar(0, 1);
        IntVar totalObj1 = model.intVar(0, 10);
        IntVar totalObj2 = model.intVar(0, 10);
        IntVar[] totalObjs = new IntVar[]{totalObj1, totalObj2};

        // We add two knapsack constraints to the solver
        // Beware : call the post() method to save it
        model.knapsack(occurrences, weight, totalObj1, weights, obj1).post();
        model.knapsack(occurrences, weight, totalObj2, weights, obj2).post();

        float timeout = 3200;
        executeToyModel(model, totalObjs, timeout);
    }

    private static void executeToyModel(Model model, IntVar[] totalObjs, float timeout){
        Region[] regions = new Region[3];
        regions[0] = new Region(new int[]{2, 4}, new int[]{1, 3});
        regions[1] = new Region(new int[]{4, 10}, new int[]{0, 1});
        regions[2] = new Region(new int[]{0, 2}, new int[]{3, 10});

        Solver solver = model.getSolver();
        int idArea = 0;
        boolean timeoutReached = false;

        while (idArea < 3 && !timeoutReached){
            // Constraints for the regions
            ArrayList<Constraint> constraintObjectives = new ArrayList<>();
            for (int i = 0; i < totalObjs.length; i++) {
                int[] objRange = regions[idArea].objRange[i];
                constraintObjectives.add(solver.getModel().arithm(totalObjs[i], ">", objRange[0]));
                constraintObjectives.add(solver.getModel().arithm(totalObjs[i], "<", objRange[1]));
            }
            idArea++;
            for (Constraint constraint: constraintObjectives){
                constraint.post();
            }

            Solution solution = new Solution(solver.getModel());
            if (solver.solve()){
                solution.record();
            }
            // Get statistics
//            recorderList.add(solver.getMeasures().toString());
            if (solution.exists()){
                System.out.println("Found a solution " + solution.getIntVal(totalObjs[0]) + " " + solution.getIntVal(totalObjs[1]));
            }else{
                // todo delete this else
                System.out.println("No solution found");
            }

            for (Constraint constraint : constraintObjectives){
                if (constraint != null){
                    solver.getModel().unpost(constraint);
                }
            }

            // reset to the initial state
            if (solver.isStopCriterionMet()){
                timeoutReached = true;
            }else{
                float elapsedTime = solver.getTimeCount();
                timeout = timeout - elapsedTime;
                solver.reset();
                solver.limitTime(timeout + "s");
            }
        }
    }
}

class Region{
    int[] obj1Range;
    int[] obj2Range;
    int[][] objRange;

    public Region(int[] obj1Range, int[] obj2Range){
        this.objRange = new int[][]{obj1Range, obj2Range};
//        this.obj1Range = rightCorner;
//        this.obj2Range = leftCorner;
    }
}
