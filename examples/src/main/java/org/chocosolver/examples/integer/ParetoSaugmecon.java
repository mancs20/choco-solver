package org.chocosolver.examples.integer;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Stream;

public class ParetoSaugmecon {
    private Constraint[] constraintObjectives;
    private int[] bestObjectiveValues;
    private Solution[] bestObjectiveValuesSolution;
    private int[] nadirObjectiveValues;
    private float timeout;
    private Solver solver;
    private Model model;
    private IntVar[] objectives;
    private IntVar saugmeconObjective;
    private final List<Solution> solutions = new ArrayList<>();
    private final List<String> recorderList = new ArrayList<>();
    private boolean stopCriterionReached;
    private boolean performLexicographicOptimization;
    private boolean cannotUseSaugmeconObjective;

    public ParetoSaugmecon(boolean performLexicographicOptimization) {
        this.performLexicographicOptimization = performLexicographicOptimization;
    }

    public Object[] run(Model model, IntVar[] objectives, boolean maximize, int timeout) {
        long startTime = System.nanoTime();
        this.timeout = timeout;
        this.model = model;
        solver = this.model.getSolver();
        // transform the problem to maximization
        this.objectives = Stream.of(objectives).map(o -> maximize ? o : model.neg(o)).toArray(IntVar[]::new);
        constraintObjectives = new Constraint[objectives.length - 1];
        // Get the best value for each objective (maximization)
        getObjectivesOptimalValues();
        getNadirObjectiveValues();
        if (!stopCriterionReached){
            // Add the saugmecon objective
            setSaugmeconObjective();
            // initialize the epsilon array, ef2 = nadir2 - 1
            int[] efArray = new int[nadirObjectiveValues.length];
            for (int i = 0; i < nadirObjectiveValues.length; i++) {
                efArray[i] = nadirObjectiveValues[i] - 1;
            }
            // initialize rwv
            int[] rwv;
            if (objectives.length == 2) {
                // if there is only 2 objectives, the rwv is empty
                rwv = new int[]{};
            }else{
                rwv = new int[bestObjectiveValues.length-2];
                System.arraycopy(bestObjectiveValues, 1, rwv, 0, rwv.length);
            }

            Set<String> previousSolutions = new HashSet<>();
            List<SolutionEfArrayInformation> previousSolutionInformation = new ArrayList<>();
            saugmeconLoop(efArray, rwv, bestObjectiveValues.length, previousSolutionInformation, previousSolutions);
            // remove the solutions that are dominated by the front
            if (!performLexicographicOptimization && (cannotUseSaugmeconObjective || objectives.length > 2)){
                for (int i = solutions.size()-1; i > -1; i--) {
                    if (solutionKisDominatedByTheFront(solutions.get(i), solutions, i)) {
                        recorderList.set(i, "No solution" + recorderList.get(i));
                        solutions.remove(i);
                    }
                }
            }
            // check if the timeout is reached, if not all the solutions are found
            if (stopCriterionReached) {
                System.out.println("Stop criterion reached, the Pareto front is incomplete");
                // check if the elements in recorderList that were found while optimizing individual objectives should
                // be removed from the Pareto front approximation
                int indexInsert = 0;
                for (int i = 0; i < bestObjectiveValuesSolution.length; i++) {
                    // set k to -1 to check if a solution that doesn't belong to the front is dominated by the front
                    if (!solutionKisDominatedByTheFront(bestObjectiveValuesSolution[i], solutions, -1)) {
                        // if the solution is not dominated by the front, add it to the front at index i
                        solutions.add(indexInsert, bestObjectiveValuesSolution[i]);
                        indexInsert++;
                    }else{
                        // if the solution is dominated by the front, remove it from the recorderList
                        recorderList.set(indexInsert, "No solution" + recorderList.get(indexInsert));
                    }
                }
            }else{
                // remove the elements in recorderList that were found while optimizing individual objectives
                for (int i = 0; i < bestObjectiveValues.length; i++) {
                    recorderList.set(i, "No solution" + recorderList.get(i));
                }
            }
        }else{
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                if (bestObjectiveValuesSolution[i] != null) {
                    solutions.add(bestObjectiveValuesSolution[i]);
                }
            }
        }
        long endTime = System.nanoTime();
        float elapsedTime = (float) (endTime - startTime) / 1_000_000_000;
        return new Object[]{solutions, recorderList, elapsedTime};
    }

    private void saugmeconLoop(int[] efArray, int[] rwv, int idObjective,
                               List<SolutionEfArrayInformation> previousSolutionInformation, Set<String> previousSolutions) {
        if (stopCriterionReached) {
            return;
        }
        idObjective -= 1;
        while (efArray[idObjective] < bestObjectiveValues[idObjective]) {
            if (idObjective == 0) {
                while (efArray[idObjective] < bestObjectiveValues[idObjective] && !stopCriterionReached) {
                    efArray[idObjective] = efArray[idObjective] + 1;
                    solveSaugmeconMostInnerLoop(efArray, rwv, previousSolutionInformation, previousSolutions);
                }
                return;
            } else {
                efArray[idObjective] = efArray[idObjective] + 1;
                saugmeconLoop(efArray, rwv, idObjective, previousSolutionInformation, previousSolutions);
                efArray[idObjective - 1] = nadirObjectiveValues[idObjective - 1] - 1;
            }
        }
        idObjective += 1;
    }

    private void solveSaugmeconMostInnerLoop(int[] efArray, int[] rwv, List<SolutionEfArrayInformation> previousSolutionInformation, Set<String> previousSolutions) {
        boolean exitFromLoopWithAcceleration = false;
        int[] solutionObjectiveValues = new int[objectives.length];
        // check if there are previous solutions that satisfy the constraints
        SolutionEfArrayInformation previousSolutionSatisfyCurrentConstraint = searchPreviousSolutionsRelaxation(efArray, previousSolutionInformation);
        if (previousSolutionSatisfyCurrentConstraint != null) {
            if (previousSolutionSatisfyCurrentConstraint.isFeasible()) {
                solutionObjectiveValues = previousSolutionSatisfyCurrentConstraint.getSolution();
            } else{
                // the previous solution is infeasible
                exitFromLoopWithAcceleration = true;
            }
        }else {
            // update right-hand side values (rhs) for the objective constraints
            updateObjectiveConstraints(efArray);
            Solution solution = optimizeIntVar(saugmeconObjective, true, true, true);
            if (stopCriterionReached){
                if (solution != null) {
                    solutions.add(solution);
                }
                return;
            }
            if (solution == null) {
                // the problem is infeasible
                exitFromLoopWithAcceleration = true;
                // save solution information
                saveSolutionInformation(efArray, solutionObjectiveValues,  previousSolutionInformation);
            } else {
                for (int i = 0; i < objectives.length; i++) {
                    solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
                }
                String solutionString = Arrays.toString(solutionObjectiveValues);
                if (!previousSolutions.contains(solutionString)) {
                    previousSolutions.add(solutionString);
                    saveSolutionInformation(efArray, solutionObjectiveValues,  previousSolutionInformation);
                    // add solution to the front
                    solutions.add(solution);
                }
            }
        }
        if (exitFromLoopWithAcceleration) {
            // todo exit from loop with acceleration method
            exitFromLoop(efArray);
        } else {
            efArray[0] = solutionObjectiveValues[1];
            // todo update rwv
            updateRwv(rwv, solutionObjectiveValues);
        }
    }

    public static SolutionEfArrayInformation searchPreviousSolutionsRelaxation(int[] efArrayActual, List<SolutionEfArrayInformation> previousSolutionInformation){
        // todo get value of the previous solution here
//        getCloserRelaxation(efArrayActual, previousSolutionInformation);
        SolutionEfArrayInformation previousSolution;
        int idPreviousCloserRelaxation = getLessConstrainedPreviousSolutions(efArrayActual, previousSolutionInformation);
        if (idPreviousCloserRelaxation != -1) {
            previousSolution = previousSolutionInformation.get(idPreviousCloserRelaxation);
        }else{
            previousSolution = null;
        }
        return previousSolution;
    }

    private static int getLessConstrainedPreviousSolutions(int[] efArrayActual, List<SolutionEfArrayInformation> previousSolutionInformation){
        if (previousSolutionInformation.isEmpty()) {
            return -1;
        }
        int idx = bisectLeftPreviousSolutionsSortedDescending(efArrayActual, previousSolutionInformation) - 1;
        boolean solutionWithMoreRelaxationFound = false;
        while (!solutionWithMoreRelaxationFound && idx > -1) {
            if (efArray1LessConstraintEfArray2(previousSolutionInformation.get(idx).getEfArray(), efArrayActual)) {
                int[] fSolutionValues = previousSolutionInformation.get(idx).getSolution();
                solutionWithMoreRelaxationFound = true;
                if (previousSolutionInformation.get(idx).isFeasible()) {
                    int[] fSolutionValuesForConstraint = Arrays.copyOfRange(fSolutionValues, 1, fSolutionValues.length);
                    if (!solutionSatisfyEfArr(fSolutionValuesForConstraint, efArrayActual)) {
                        solutionWithMoreRelaxationFound = false;
                        idx -= 1;
                    }
                }
            } else {
                idx -= 1;
            }
        }
        return idx;
    }

    private static void saveSolutionInformation(int[] efArrayActual, int[] solutionObjectiveValues, List<SolutionEfArrayInformation> previousSolutionInformation) {
        int lo = idInsortLeftPreviousSolutions(efArrayActual, previousSolutionInformation);
        // translate this python code to java previous_solution_information.insert(lo, [ef_array_to_insert, solution])
        boolean feasible = solutionObjectiveValues != null;
        SolutionEfArrayInformation solutionEfArrayInformation = new SolutionEfArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible);
        previousSolutionInformation.add(lo, solutionEfArrayInformation);
    }

    private static int idInsortLeftPreviousSolutions(int[] efArrayActual, List<SolutionEfArrayInformation> previousSolutionInformation) {
        return bisectLeftPreviousSolutionsSortedDescending(efArrayActual, previousSolutionInformation);
    }

    private static int bisectLeftPreviousSolutionsSortedDescending(int[] efArrayActual, List<SolutionEfArrayInformation> previousSolutionInformation) {
        int lo = 0;
        int hi = previousSolutionInformation.size();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (efArray1LessConstraintEfArray2(efArrayActual, previousSolutionInformation.get(mid).getEfArray())) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    private static boolean efArray1LessConstraintEfArray2(int[] efArray1, int[] efArray2) {
        boolean lessConstrained = true;
        for (int i = 0; i < efArray1.length; i++) {
            if (efArray1[i] > efArray2[i]) {
                lessConstrained = false;
                break;
            }
        }
        return lessConstrained;
    }

    private static boolean solutionSatisfyEfArr(int[] solutionValues, int[] efArray) {
        boolean satisfy = true;
        for (int i = 0; i < solutionValues.length; i++) {
            if (solutionValues[i] < efArray[i]) {
                satisfy = false;
                break;
            }
        }
        return satisfy;
    }

    private void getObjectivesOptimalValues() {
        bestObjectiveValuesSolution = new Solution[objectives.length - 1];
        bestObjectiveValues = findSolutionsConsideringOneObjective(true, true);
    }

    private void getNadirObjectiveValues() {
        nadirObjectiveValues = findSolutionsConsideringOneObjective(false, false);
    }

    private int[] findSolutionsConsideringOneObjective(boolean maximize, boolean searchForBestObjectivesValues) {
        // todo modify the code to deal with more than one objective
        if (stopCriterionReached) {
            return new int[]{};
        }
        int[] objectivesValues = new int[objectives.length - 1];
        for (int i = 1; i < objectives.length; i++) {
            IntVar objective = objectives[i];
            Solution solution = optimizeIntVar(objective, maximize, searchForBestObjectivesValues, false);
            if (solution != null) {
                objectivesValues[i - 1] = solution.getIntVal(objective);
                if (searchForBestObjectivesValues) {
                    bestObjectiveValuesSolution[i - 1] = solution;
                }
            } else {
                break;
            }
        }
        return objectivesValues;
    }

    private Solution optimizeIntVar(IntVar objective, boolean maximize, boolean saveStats, boolean optimizeSaugmeconObjective) {
        Solution solution = null;
        if (timeout <= 0) {
            stopCriterionReached = true;
        }else{
            solver.limitTime(timeout + "s");
            if (!solver.isStopCriterionMet()) {
                if (optimizeSaugmeconObjective && cannotUseSaugmeconObjective){
                    if (performLexicographicOptimization){
                        solution = solver.findLexOptimalSolution(objectives, maximize);
                    }else{
                        solution = solver.findOptimalSolution(objectives[0], maximize);
                    }
                }else{
                    solution = solver.findOptimalSolution(objective, maximize);
                }

                timeout = timeout - solver.getTimeCount();
                if (solution != null && saveStats) {
                    recorderList.add(solver.getMeasures().toString());
                }
                if (!solver.isStopCriterionMet()){
                    solver.reset();
                }else {
                    stopCriterionReached = true;
                }
            }
        }
        return solution;
    }

    private void setSaugmeconObjective() {
        // check if the saugmecon objective can be calculated as in the paper. If the objectives are to big,
        // the coefficients in the objective function will exceed the int limit. In this case, there are two options:
        // 1. optimize objective 1 and at the end check if there are some solutions that do not belong to the pareto
        // front. This could happen if for the same optimal value of objective 1, there are more than value for
        // objective i. For example, if the optimal value of objective 1 is 10, and there are two solutions with
        // objective 2 values 5 and 6, is possible that the solution obtained by the solver is 10, 6, that solution
        // will be added to the front and then 10,5 will be found. In this case, the solution 10,6 should be removed
        // from the front.
        // 2. Apply lexicographic optimization to avoid the situation explained above. In this way the solver only
        // return 10,5.
        cannotUseSaugmeconObjective = false;
        int lbSaugmeconObjective = 0;
        int ubSaugmeconObjective = 0;

        // obj = f1 + eps * (f2/r2 + ... + fn/rn)
        // as we are using integer values, we can use the following formula range_multiplier = (r2*...rn) and 1/eps
        // obj = f1 * range_multiplier * (1 / eps) + range_multiplier * (f2/r2 + ... + fn/rn)
        // calculate the range for each objective
        int[] range = new int[bestObjectiveValues.length];
        for (int i = 0; i < bestObjectiveValues.length; i++) {
            range[i] = Math.abs(bestObjectiveValues[i] - nadirObjectiveValues[i]);
        }
        int rangeMultiplier = 1;
        for (int rangeI : range) {
            if (rangeMultiplier > Integer.MAX_VALUE / rangeI) {
                cannotUseSaugmeconObjective = true;
                lbSaugmeconObjective = objectives[0].getLB();
                ubSaugmeconObjective = objectives[0].getUB();
                break;
            }
            rangeMultiplier *= rangeI;
        }

        int[] coefficients = new int[objectives.length];
        if (!cannotUseSaugmeconObjective) {
            // calculate the 1/eps value
            // eps <= (1 / (f2_max/r2 + ... + fn_max/rn))
            // (f2_max/r2 + ... + fn_max/rn) <= 1/eps
            // 1/eps >= (f2_max/r2 + ... + fn_max/rn) + k, where k is a small value, for integer values we can use 1
            double inverseEps = 1.0;
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                inverseEps += (float) Math.max(bestObjectiveValues[i], nadirObjectiveValues[i]) / (float) range[i];
            }
            //saugmeconObjective = f1 * range_multiplier * (1 / eps) + range_multiplier * (f2/r2 + ... + fn/rn)
            coefficients[0] = (int) (rangeMultiplier * inverseEps);
            for (int i = 1; i < objectives.length; i++) {
                coefficients[i] = rangeMultiplier / range[i - 1];
            }

            for (int i = 0; i < objectives.length; i++) {
                if (Math.abs((long)((Integer.MAX_VALUE - lbSaugmeconObjective) / coefficients[i])) < Math.max(Math.abs(objectives[i].getUB()), Math.abs(objectives[i].getLB()))) {
                    cannotUseSaugmeconObjective = true;
                    lbSaugmeconObjective = objectives[0].getLB();
                    ubSaugmeconObjective = objectives[0].getUB();
                    break;
                }
                lbSaugmeconObjective += coefficients[i] * objectives[i].getLB();
                ubSaugmeconObjective += coefficients[i] * objectives[i].getUB();
            }
        }
        if (!cannotUseSaugmeconObjective) {
            saugmeconObjective = model.intVar("saugmeconObjective", lbSaugmeconObjective, ubSaugmeconObjective);
            IntVar[] saugmeconObjectiveArr = new IntVar[objectives.length];
            System.arraycopy(objectives, 0, saugmeconObjectiveArr, 0, objectives.length);
            model.scalar(saugmeconObjectiveArr, coefficients, "=", saugmeconObjective).post();
        }else{
            solver.getModel().getObjective().getModel().clearObjective();
        }
    }

    private void addObjectivesAsConstraints(int[] efArray) {
        for (int i = 0; i < constraintObjectives.length; i++) {
            constraintObjectives[i] = model.arithm(objectives[i + 1], ">=", efArray[i]);
            constraintObjectives[i].post();
        }
    }

    private void updateObjectiveConstraints(int[] efArray) {
        // unpost constraints
        if (constraintObjectives[0] != null) {
            for (Constraint constraint : constraintObjectives) {
                model.unpost(constraint);
            }
        }
        // add new constraints
        addObjectivesAsConstraints(efArray);
    }

    private void exitFromLoop(int[] efArray){
        int i = 0;
        while ((i < efArray.length - 1) && (efArray[i] == nadirObjectiveValues[i])) {
            i++;
        }
        if (i + 1 >= 0) System.arraycopy(bestObjectiveValues, 0, efArray, 0, i + 1);
    }

    private void updateRwv(int[] rwv, int[] newSolutionValues){
        for (int i = 0; i < rwv.length; i++) {
            if (newSolutionValues[i+2] < rwv[i]) {
                rwv[i] = newSolutionValues[i];
            }
        }
    }

    public boolean solutionKisDominatedByTheFront(Solution newSolution, List<Solution> front, int k) {
        boolean newSolutionIsDominated = false;
        // at this point is possible that the
        for (int i = 0; i < front.size(); i++) {
            if (i != k){
                if (solutionADominatesB(front.get(i), newSolution)) {
                    newSolutionIsDominated = true;
                    break;
                }
            }
        }
        return newSolutionIsDominated;
    }

    private boolean solutionADominatesB(Solution solutionA, Solution solutionB) {
        boolean dominates = true;
        for (IntVar objective : objectives) {
            if (solutionA.getIntVal(objective) < solutionB.getIntVal(objective)) {
                dominates = false;
                break;
            }
        }
        return dominates;
    }
}

class SolutionEfArrayInformation {
    private final int[] solution;
    private final int[] efArray;
    private final boolean feasible;

    public SolutionEfArrayInformation(int[] solution, int[] efArray, boolean feasible) {
        this.solution = solution;
        this.efArray = efArray;
        this.feasible = feasible;
    }

    public int[] getSolution() {
        return solution;
    }

    public int[] getEfArray() {
        return efArray;
    }

    public boolean isFeasible() {
        return feasible;
    }
}
