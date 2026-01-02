package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.moexperiments.TimeBasedSolutionPrinter;
import org.chocosolver.util.moexperiments.TimeoutHolder;

import java.math.BigInteger;
import java.util.*;


public class SaugmeconNoRecursion extends ParetoAbstract implements TimeoutHolder {


    private final Constraint[] constraintObjectives;
    private int[] bestObjectiveValues;
    private Solution[] bestObjectiveValuesSolution;
    private int[] nadirObjectiveValues;
    private final float timeout;
    private final long startTime;
    private final Solver solver;
    private final Model model;
    private final IntVar[] objectives;
    private boolean stopCriterionReached;
    private final boolean performLexicographicOptimization;
    private boolean cannotUseSaugmeconObjective;
    private boolean useRealObjectiveFunction;
    protected int solveCallsCount;
    private final TimeBasedSolutionPrinter recorder;
    private int[] efArray;
    private int[] rwv;
    private Set<String> previousSolutions;
    private List<SolutionEfArrayInformation> previousSolutionInformation;

    public SaugmeconNoRecursion(boolean performLexicographicOptimization, Model model, IntVar[] objectives, int timeout){
        this.performLexicographicOptimization = performLexicographicOptimization;
        solveCallsCount = 0;
        startTime = System.nanoTime();
        recorder = new TimeBasedSolutionPrinter();
        this.timeout = timeout;
        this.model = model;
        solver = this.model.getSolver();
        // transform the problem to maximization
        this.objectives = objectives;
        constraintObjectives = new Constraint[objectives.length - 1];
        exhaustive = true;
    }

    public SaugmeconNoRecursion(boolean performLexicographicOptimization, Model model, IntVar[] objectives, int timeout,
    boolean useRealObjectiveFunction){
        this(performLexicographicOptimization, model, objectives, timeout);
        this.cannotUseSaugmeconObjective = !useRealObjectiveFunction;
        this.useRealObjectiveFunction = useRealObjectiveFunction;
    }

    public void initialization() {
        getObjectivesOptimalValues();
        getNadirObjectiveValues();
        if (useRealObjectiveFunction) {
            setSaugmeconObjectiveUsingReal();
        } else {
            setSaugmeconObjective();
        }

        // initialize the epsilon array
        efArray = new int[nadirObjectiveValues.length];
        System.arraycopy(nadirObjectiveValues, 0, efArray, 0, nadirObjectiveValues.length);
        // initialize rwv
        rwv = new int[bestObjectiveValues.length];
        System.arraycopy(bestObjectiveValues, 0, rwv, 0, rwv.length);
        previousSolutions = new HashSet<>();
        previousSolutionInformation = new ArrayList<>();
    }

    private void getObjectivesOptimalValues() {
        bestObjectiveValuesSolution = new Solution[objectives.length - 1];
        bestObjectiveValues = findSolutionsConsideringOneObjective(true, true);
    }

    private void getNadirObjectiveValues() {
        // to obtain the nadir values it is faster to use the lower bound of the objectives instead of computing the
        // inverse optimal values
        nadirObjectiveValues = new int[objectives.length - 1];
        for (int i = 1; i < objectives.length; i++) {
            nadirObjectiveValues[i - 1] = objectives[i].getLB();
        }
//        nadirObjectiveValues = findSolutionsConsideringOneObjective(false, false);
    }

    private int[] findSolutionsConsideringOneObjective(boolean maximize, boolean searchForBestObjectivesValues) {
        if (stopCriterionReached) {
            return new int[]{};
        }
        int[] objectivesValues = new int[objectives.length - 1];
        for (int i = 1; i < objectives.length; i++) {
            IntVar objective = objectives[i];
            model.setObjective(maximize, objective);
            Solution solution = optimizeIntVar(maximize, false, false);
            if (solution != null) {
                objectivesValues[i - 1] = solution.getIntVal(objective);
                if (searchForBestObjectivesValues) {
                    bestObjectiveValuesSolution[i - 1] = solution;
                    // add a constraint to limit UB of the objective value
                    model.arithm(objective, "<=", solution.getIntVal(objective)).post();
                }
            } else {
                break;
            }
        }
        return objectivesValues;
    }

    private Solution optimizeIntVar(boolean maximize, boolean saveStats, boolean optimizeSaugmeconObjective) {
        Solution solution = null;
        float remainingTimeout = updateSolverTimeoutCurrentTime(solver, timeout, startTime);
        if (remainingTimeout == 0) {
            stopCriterionReached = true;
        }else{
            if (!solver.isStopCriterionMet()) {
                if (saveStats) {
                    recorder.setFirstSolution(true);
                }
                solution = new Solution(model);
                if (optimizeSaugmeconObjective && cannotUseSaugmeconObjective){
                    if (performLexicographicOptimization){
                        solution = solver.findLexOptimalSolution(objectives, maximize, recorder);
                    }else{
                        while (solver.solve()){
                            solution.record();
                            recorder.onNewSolution(solution, objectives);
                        }
                    }
                }else{
                    while (solver.solve()){
                        solution.record();
                        if (saveStats){
                            recorder.onNewSolution(solution, objectives);
                        }
                    }
                }
                solver.removeStopCriterion();
                recorderList.add(solver.getMeasures().toString());
                if (solution == null || !solution.exists()) solution = null;
                if (!solver.isStopCriterionMet()){
                    solver.reset();
                    solveCallsCount++;
                    solver.getMeasures().setRestartCount(solveCallsCount);
                }else {
                    stopCriterionReached = true;
                }
            }
        }
        return solution;
    }

    private void setSaugmeconObjective() {
        // check if the saugmecon objective can be calculated as in the paper. If the objectives are too big,
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
        int[] coefficients = new int[objectives.length];

        if (objectives.length > 2) {
            // obj = f1 + eps * (f2/r2 + ... + fp/rp)
            // as we are using integer values, we can use the following formula range_multiplier = (r2*...rp) and 1/eps
            // obj = f1 * range_multiplier * (1 / eps) + range_multiplier * (f2/r2 + ... + fp/rp)
            // calculate the range for each objective
            int[] range = new int[bestObjectiveValues.length];
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                range[i] = Math.abs(bestObjectiveValues[i] - nadirObjectiveValues[i]);
            }
            int rangeMultiplier;
            rangeMultiplier = (int) lcm(range);
            if (rangeMultiplier >= Integer.MAX_VALUE) {
                cannotUseSaugmeconObjective = true;
            }

            if (!cannotUseSaugmeconObjective) {
                // calculate the 1/eps value
                // eps <= (1 / (f2_max/r2 + ... + fn_max/rp))
                // (f2_max/r2 + ... + fn_max/rp) <= 1/eps
                // 1/eps >= (f2_max/r2 + ... + fn_max/rp) + k, where k is a small value, for integer values we can use 1
                double inverseEps = objectives.length; // minimum value for 1/eps is the number of objectives
                //saugmeconObjective = f1 * range_multiplier * (1 / eps) + range_multiplier * (f2/r2 + ... + fp/rp)
                coefficients[0] = (int) (rangeMultiplier * inverseEps);
                for (int i = 1; i < objectives.length; i++) {
                    coefficients[i] = rangeMultiplier / range[i - 1];
                }

                for (int i = 0; i < objectives.length; i++) {
                    if (Math.abs((long) ((Integer.MAX_VALUE) / coefficients[i])) <= Math.max(Math.abs(objectives[i].getUB()), Math.abs(objectives[i].getLB()))) {
                        cannotUseSaugmeconObjective = true;
                        break;
                    }
                    lbSaugmeconObjective += coefficients[i] * objectives[i].getLB();
                    ubSaugmeconObjective += coefficients[i] * objectives[i].getUB();
                }
                if (Math.max(Math.abs(ubSaugmeconObjective), Math.abs(lbSaugmeconObjective)) >= Integer.MAX_VALUE) {
                    cannotUseSaugmeconObjective = true;
                }
            }
        } else {
            coefficients[0] = Math.abs(objectives[1].getUB() - objectives[1].getLB()) + 1;
            coefficients[1] = 1;
            if (Math.abs((long) ((Integer.MAX_VALUE) / coefficients[0])) <= Math.max(Math.abs(objectives[0].getUB()), Math.abs(objectives[0].getLB()))) {
                cannotUseSaugmeconObjective = true;
            } else {
                for (int i = 0; i < objectives.length; i++) {
                    lbSaugmeconObjective += coefficients[i] * objectives[i].getLB();
                    ubSaugmeconObjective += coefficients[i] * objectives[i].getUB();
                }
                if (Math.max(Math.abs(ubSaugmeconObjective), Math.abs(lbSaugmeconObjective)) >= Integer.MAX_VALUE) {
                    cannotUseSaugmeconObjective = true;
                }
            }
        }
        if (!cannotUseSaugmeconObjective) {
            IntVar saugmeconObjective = model.intVar("saugmeconObjective", lbSaugmeconObjective, ubSaugmeconObjective);
            IntVar[] saugmeconObjectiveArr = new IntVar[objectives.length];
            System.arraycopy(objectives, 0, saugmeconObjectiveArr, 0, objectives.length);
            model.scalar(saugmeconObjectiveArr, coefficients, "=", saugmeconObjective).post();
            model.setObjective(true, saugmeconObjective);
        }else{
            solver.getModel().getObjective().getModel().clearObjective();
            System.out.println("Lexicographic optimization is used. Saugmecon objective is bigger than Integer.MAX_VALUE");
        }
    }

    private void setSaugmeconObjectiveUsingReal() {
        // calculate the range for each objective
        double[] range = new double[bestObjectiveValues.length];
        double maxRange = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bestObjectiveValues.length; i++) {
            range[i] = Math.abs(bestObjectiveValues[i] - nadirObjectiveValues[i]);
            if (range[i] > maxRange) {
                maxRange = range[i];
            }
        }
        double delta;
        if (objectives.length > 2) {
            delta = Math.nextDown(1.0d / objectives.length);
        } else {
            delta = Math.nextDown(1.0d / (range[0]));
        }

        double[] coefficients = new double[objectives.length+1];
        coefficients[0] = 1d;
        if (objectives.length > 2) {
            for (int i = 1; i < objectives.length; i++) {
                coefficients[i] = delta / (range[i - 1]);
            }
        } else {
            coefficients[1] = delta;
        }

        double lbSaugmeconObjective = 0;
        double ubSaugmeconObjective = 0;
        for (int i = 0; i < objectives.length; i++) {
            lbSaugmeconObjective += coefficients[i] * objectives[i].getLB();
            ubSaugmeconObjective += coefficients[i] * objectives[i].getUB();
        }

        // the smallest difference between two objective values is when one objective changes by 1 and the rest remain
        // the same. In that case, when we subtract the two saugmecon objective values, the difference is
        // delta x (o_i+1)/range_i - delta x o_i/range_i (i is the objective that have change).
        // delta x (o_i+1)/range_i - delta x o_i/range_i = delta/range_i >= delta/maxRange
        // thus we can set the precision to the minimum difference 1/maxRange
        if (maxRange <= 0d) {
            throw new IllegalStateException("maxRange must be > 0 for SAUGMECON precision");
        }
        double precisionSaugmecon;
        if (objectives.length > 2) {
            precisionSaugmecon = Math.nextDown(delta / maxRange);
        } else {
            precisionSaugmecon = Math.nextDown(delta);
        }

        RealVar saugmeconObjective = model.realVar("saugmeconObjective", lbSaugmeconObjective,
                ubSaugmeconObjective, precisionSaugmecon);

        Variable[] vars = new Variable[objectives.length + 1];
        System.arraycopy(objectives, 0, vars, 0, objectives.length);
        vars[objectives.length] = saugmeconObjective;
        coefficients[coefficients.length-1] = -1d;
        model.scalar(vars, coefficients, "=", 0).post();
        // precision objective
        model.setPrecision(precisionSaugmecon);
        model.setObjective(true, saugmeconObjective);
    }

    public List<Solution> exploreAllEpsilonValues() {
        while (!stopCriterionReached && efArray[efArray.length-1] <= bestObjectiveValues[bestObjectiveValues.length-1]){
            int[] solutionObjectiveValues = getSolutionForCurrentEpsilonValues();
            if (!stopCriterionReached) {
                if (solutionObjectiveValues != null){
                    updateRelativeWorstValues(solutionObjectiveValues);
                } else {
                    earlyExitAfterInfeasibility();
                }
                updateEpsilonValues();
            }
        }
        recorder.onEnd();
        allSolutions = new ArrayList<>(solutions);
        if (stopCriterionReached) {
            exhaustive = false;
            System.out.println("Stop criterion reached, the Pareto front may be incomplete");
            addBestObjectiveValuesAsSolutionIfNotDominated();
            removeLastSolutionIfDominated();
        } else {
            for (int i = 0; i < objectives.length; i++) {
                recorderList.set(i, "Preprocessing solution" + recorderList.get(i));
            }
        }
        for (int i = 0; i < bestObjectiveValues.length; i++) {
            if (bestObjectiveValuesSolution[i] != null) {
                allSolutions.add(i, bestObjectiveValuesSolution[i]);
            }
        }
        return solutions;
    }

    private int[] getSolutionForCurrentEpsilonValues() {
        int[] solutionObjectiveValues = null;
        // set the upper bounds for all objectives except the first one
        SolutionEfArrayInformation previousSolutionSatisfyCurrentConstraint = searchPreviousSolutionsRelaxation(efArray, previousSolutionInformation);
        if (previousSolutionSatisfyCurrentConstraint != null) {
            // uncomment for debugging
//            System.out.print(" is satisfied by a previous solution: ");
            if (previousSolutionSatisfyCurrentConstraint.isFeasible()) {
                solutionObjectiveValues = previousSolutionSatisfyCurrentConstraint.getSolution();
                // uncomment for debugging
//                System.out.println(Arrays.toString(solutionObjectiveValues) + " efArrayPrevious: " +
//                        Arrays.toString(previousSolutionSatisfyCurrentConstraint.getEfArray()));
            }
        } else {
            // update right-hand side values (rhs) for the objective constraints
            updateObjectiveConstraints();
            Solution solution = optimizeIntVar(true, false, true);
            if (stopCriterionReached){
                if (solution != null) {
                    solutions.add(solution);
                }
            } else {
                if (solution == null) {
                    // save solution information
                    saveSolutionInformation(efArray, null,  previousSolutionInformation);
                    // uncomment for debugging
//                System.out.println(" after solved is infeasible");
                } else {
                    solutionObjectiveValues = new int[objectives.length];
                    for (int i = 0; i < objectives.length; i++) {
                        solutionObjectiveValues[i] = solution.getIntVal(objectives[i]);
                    }
                    // uncomment for debugging
//                System.out.println(" after solved is feasible: " + Arrays.toString(solutionObjectiveValues));
                    String solutionString = Arrays.toString(solutionObjectiveValues);
                    if (!previousSolutions.contains(solutionString)) {
                        previousSolutions.add(solutionString);
                        // add solution to the front
                        solutions.add(solution);
                    } //else { // uncomment for debugging
//                    System.out.println("Above solution already in the front");
//                }
                    saveSolutionInformation(efArray, solutionObjectiveValues,  previousSolutionInformation);
                }
            }
        }
        return solutionObjectiveValues;
    }

    public static SolutionEfArrayInformation searchPreviousSolutionsRelaxation(int[] efArrayActual, List<SolutionEfArrayInformation> previousSolutionInformation){
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
        int idx = previousSolutionInformation.size() - 1;
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

    private static void saveSolutionInformation(int[] efArrayActual, int[] solutionObjectiveValues, List<SolutionEfArrayInformation> previousSolutionInformation) {
        boolean feasible = solutionObjectiveValues != null;
        SolutionEfArrayInformation solutionEfArrayInformation = new SolutionEfArrayInformation(solutionObjectiveValues, efArrayActual.clone(), feasible);
        previousSolutionInformation.add(solutionEfArrayInformation);
    }

    private void updateObjectiveConstraints() {
        // unpost constraints
        if (constraintObjectives[0] != null) {
            for (Constraint constraint : constraintObjectives) {
                model.unpost(constraint);
            }
        }
        // add new constraints
        addObjectivesAsConstraints();
    }

    private void addObjectivesAsConstraints() {
        for (int i = 0; i < constraintObjectives.length; i++) {
            constraintObjectives[i] = model.arithm(objectives[i + 1], ">=", efArray[i]);
            constraintObjectives[i].post();
        }
    }

    private void updateRelativeWorstValues(int[] solutionObjectiveValues) {
        rwv[0] = solutionObjectiveValues[1];
        if (objectives.length > 2) {
            for (int i = 1; i < rwv.length; i++) {
                rwv[i] = Math.min(rwv[i], solutionObjectiveValues[i+1]);
            }
        }
    }

    private void earlyExitAfterInfeasibility(){
        int j = efArray.length - 1;
        for (int i = 0; i < efArray.length - 1; i++) {
            if (efArray[i] != nadirObjectiveValues[i]) {
                j = i;
                break;
            }
        }
        System.arraycopy(bestObjectiveValues, 0, efArray, 0, j + 1);
    }

    private void updateEpsilonValues(){
        for (int i = 0; i < efArray.length; i++) {
            if (efArray[i] < bestObjectiveValues[i] && rwv[i] < bestObjectiveValues[i]) {
                efArray[i] = rwv[i] + 1;
                rwv[i] = bestObjectiveValues[i];
                break;
            } else if (i == efArray.length - 1) {
                efArray[i] = bestObjectiveValues[i] + 1;
            } else {
                efArray[i] = nadirObjectiveValues[i];
            }
        }
    }

    private void addBestObjectiveValuesAsSolutionIfNotDominated(){
        if (!solutions.isEmpty()) {
            for (int i = 0; i < bestObjectiveValues.length; i++) {
                if (!solutionKisWeaklyDominatedByTheFront(bestObjectiveValuesSolution[i], solutions, -1)) {
                    solutions.add(i, bestObjectiveValuesSolution[i]);
                } else {
                    recorderList.set(i, "Preprocessing solution" + recorderList.get(i));
                }
            }
        } else {
            for (Solution solution : bestObjectiveValuesSolution) {
                if (solution != null) {
                    solutions.add(solution);
                }
            }
        }
    }
    private void removeLastSolutionIfDominated(){
        if (solutionKisWeaklyDominatedByTheFront(solutions.get(solutions.size()-1), solutions, solutions.size()-1)) {
            solutions.remove(solutions.size()-1);
            recorderList.set(recorderList.size()-1, "Dominated solution" + recorderList.get(recorderList.size()-1));
        }
    }

    private boolean solutionKisWeaklyDominatedByTheFront(Solution newSolution, List<Solution> front, int k) {
        boolean newSolutionIsDominated = false;
        // at this point is possible that the
        for (int i = 0; i < front.size(); i++) {
            if (i != k){
                if (solutionAWeaklyDominatesB(front.get(i), newSolution)) {
                    newSolutionIsDominated = true;
                    break;
                }
            }
        }
        return newSolutionIsDominated;
    }

    private boolean solutionAWeaklyDominatesB(Solution solutionA, Solution solutionB) {
        boolean dominates = true;
        for (IntVar objective : objectives) {
            if (solutionA.getIntVal(objective) < solutionB.getIntVal(objective)) {
                dominates = false;
                break;
            }
        }
        return dominates;
    }

    private static long lcm(long a, long b) {
        return Math.abs(a * b) / BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).longValue();
    }

    // Compute LCM of an array of numbers
    private static long lcm(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("Input array must not be empty");
        }

        long result = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            result = lcm(result, numbers[i]);
        }
        return result;
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
