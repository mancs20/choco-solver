package org.chocosolver.solver.objective.mocoframework.component.preprocessing;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.util.SolutionFinder;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.criteria.Criterion;

import java.math.BigInteger;
import java.util.*;

public class SaugmeconPreprocessing extends BasePreprocessing {
    private int[] idealValues;
    private int[] nadirValues;

    public SaugmeconPreprocessing(SolutionFinder optimizer) {
        super(optimizer);
    }

    @Override
    public void apply(Model model, IntVar[] objectives, ParetoArchive archive, StrategyParams params, Criterion... stop) {
        // custom objectives order
        int[] objOrder = getObjectivesOrder(objectives);
        Set<Integer> excludedObjectives = new HashSet<>();
        // objOrder[0] is the objective optimized with saugmecon, it is not used for ideal and nadir calculation
        excludedObjectives.add(objOrder[0]);
        idealValues = getIdealValues(objectives, archive, excludedObjectives, params, stop);
        nadirValues = getNadirValues(objectives, excludedObjectives, stop);
        int[] epsilonArr = new int[nadirValues.length];
        System.arraycopy(nadirValues, 0, epsilonArr, 0, nadirValues.length);
        params.setObjectivesOrder(objOrder);
        params.setIdealPoint(idealValues);
        params.setNadirPoint(nadirValues);
        params.setEpsilonArray(epsilonArr);
        params.setRwv(idealValues.clone());
        params.setCheckIfNewSolutionDominates(false);

        params.setLexicographicOptimizationOrder(objOrder);
        model.clearObjective();
        // constraint objectives
        for (int i = 0; i < idealValues.length; i++) {
            model.arithm(objectives[i + 1], "<=", idealValues[i]).post();
        }
    }

    private boolean validateIdealNadir(int[] idealValues, int[] nadirValues) {
        for (int i = 0; i < idealValues.length; i++) {
            if (idealValues[i] <= nadirValues[i]) {
                return false;
            }
        }
        return true;
    }

    private int[] getObjectivesOrder(IntVar[] objectives) {
        // todo for saugmecon, the order could be determined analyzing the ranges of the objectives
        int[] order = new int[objectives.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        return order;
//        int[] boundsRange = new int[objectives.length];
//        for (int i = 0; i < objectives.length; i++) {
//            boundsRange[i] = Math.abs(objectives[i].getUB() - objectives[i].getLB());
//        }
//        // sort objectives by bounds range in descending order
//        Integer[] indices = new Integer[objectives.length];
//        for (int i = 0; i < objectives.length; i++) {
//            indices[i] = i;
//        }
//        java.util.Arrays.sort(indices, (a, b) -> Integer.compare(boundsRange[b], boundsRange[a]));
//        for (int i = 0; i < objectives.length; i++) {
//            order[i] = indices[i];
//        }
//        return order;
    }

    private Constraint setSaugmeconObjective(Model model, IntVar[] objectives) {
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
        boolean cannotUseSaugmeconObjective = false;
        int lbSaugmeconObjective = 0;
        int ubSaugmeconObjective = 0;
        int[] coefficients = new int[objectives.length];

        if (objectives.length > 2) {
            // obj = f1 + eps * (f2/r2 + ... + fp/rp)
            // as we are using integer values, we can use the following formula range_multiplier = (r2*...rp) and 1/eps
            // obj = f1 * range_multiplier * (1 / eps) + range_multiplier * (f2/r2 + ... + fp/rp)
            // calculate the range for each objective
            int[] range = new int[idealValues.length];
            for (int i = 0; i < idealValues.length; i++) {
                range[i] = Math.abs(idealValues[i] - nadirValues[i]);
                if (range[i] == 0) {
                    // cannot use saugmecon objective as one of the ranges is 0
                    cannotUseSaugmeconObjective = true;
                    break;
                }
            }
            int rangeMultiplier = 0;
            long lcmValue = lcm(range);
            if (lcmValue >= Integer.MAX_VALUE) {
                cannotUseSaugmeconObjective = true;
            } else {
                rangeMultiplier = (int) lcmValue;
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
            Constraint objectiveFunction = model.scalar(objectives, coefficients, "=", saugmeconObjective);
            objectiveFunction.post();
            model.setObjective(true, saugmeconObjective);
            return objectiveFunction;
        }else{
            model.clearObjective();
            System.out.println("Lexicographic optimization is used. Saugmecon objective is bigger than Integer.MAX_VALUE");
            return null;
        }
    }

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

    private static long lcm(long a, long b) {
        return Math.abs(a * b) / BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).longValue();
    }
}
