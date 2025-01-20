package org.chocosolver.solver.objective;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class ParetoMaximizerGIAClassic extends ParetoMaximizerGIAGeneral {

//    private final List<int[]> paretoFront;
    private boolean improveSolution;
//    private KDTree paretoTree;

    public ParetoMaximizerGIAClassic(final IntVar[] objectives, boolean portfolio, PropagatorPriority priority) {
        super(objectives, priority, false, portfolio);
//        this.paretoFront = new ArrayList<>();
//        this.paretoTree = new KDTree(n);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void onSolution() {
        // get objective values
        boolean saveSolution = true;
        if (portfolio) {
            saveSolution = saveSolutionPortfolio();
        }
        if (saveSolution) {
            for (int i = 0; i < n; i++) {
                lastObjectiveVal[i] = objectives[i].getValue();
            }
            setLastSolution(new Solution(model));
            lastSolution.record();
        }
        if (!improveSolution){
            improveSolution = true;
            if (!paretoFront.isEmpty() && (boundedType == GiaConfig.BoundedType.DOMINATING_DOMINATES ||
                    boundedType == GiaConfig.BoundedType.LAZY_DOMINATING_DOMINATES)){
                setLowestUpperBound();
            }
        }
    }

    private boolean saveSolutionPortfolio() {
        boolean saveSolution = true;
        for (int i = 0; i < n; i++) {
            if (objectives[i].getValue() < lastObjectiveVal[i]) {
                saveSolution = false;
                break;
            }
        }
        return saveSolution;
    }

//    todo verify for other problems if it is better to use the propagation condition or not, for the test case it is
//     better NOT to use it
    //@Override
//    public int getPropagationConditions(int vIdx) {
//        return IntEventType.boundAndInst();
//    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (!improveSolution){
            applyGavanelliFiltering();
        }else{
            computeDominatedArea();
            if (!paretoFront.isEmpty() && (boundedType == GiaConfig.BoundedType.DOMINATING_DOMINATES ||
                    boundedType == GiaConfig.BoundedType.LAZY_DOMINATING_DOMINATES)){
                verifyLowestUpperBound();
            }
        }
    }

    private void verifyLowestUpperBound() throws ContradictionException {
        for (int i = 0; i < objectives.length; i++) {
            if (objectives[i].getUB() > lowestUpperBounds[i]) {
                objectives[i].updateUpperBound(lowestUpperBounds[i], this);
            }
        }
    }

    private void applyGavanelliFiltering() throws ContradictionException{
        if (paretoFront.isEmpty()) {
            return;
        }
        for (int i = 0; i < objectives.length; i++) {
            computeTightestPoint(i);
        }

        if(boundedType == GiaConfig.BoundedType.DOMINATING_DOMINATES){
            // filter using also the upper bound
            for (int i = 0; i < objectives.length; i++) {
                computeLowestUpperBound(i);
            }
        }
    }

    /**
     * Compute tightest point for objective i
     * i.e. the point that dominates DP_i and has the biggest obj_i
     *
     * @param i index of the variable
     */
    private void computeTightestPoint(int i) throws ContradictionException {
        int tightestPoint = Integer.MIN_VALUE;
        int[] dominatedPoint = computeDominatedPoint(i);


//        int[] minBoundary = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE};
        // Check if the new point is dominated and update boundaries
//        boolean isDominated = paretoTree.isDominated(dominatedPoint, minBoundary);


        // TODO check the point quad tree representation in the paper to avoid iterating over all the solutions
        for (int[] sol : paretoFront) {
            if (dominates(sol, dominatedPoint)) {
                int currentPoint = sol[i] + 1;
                if (tightestPoint < currentPoint) {
                    tightestPoint = currentPoint;
                }
            }
        }
        if (tightestPoint > Integer.MIN_VALUE) {
            objectives[i].updateLowerBound(tightestPoint, this);
        }
    }

    /**
     * Compute dominated point for objective i,
     * i.e. DP_i = (obj_1_max,...,obj_i_min,...,obj_m_max)
     *
     * @param i index of the variable
     * @return dominated point
     */
    private int[] computeDominatedPoint(int i) {
        int[] dp = new int[objectives.length];
        for (int j = 0; j < objectives.length; j++) {
            dp[j] = objectives[j].getUB();
        }
        dp[i] = objectives[i].getLB();
        return dp;
    }

    private void computeLowestUpperBound(int i) throws ContradictionException {
        int lowestUpperBound = Integer.MAX_VALUE;
        int[] dominatingPoint = computeDominatingPoint(i);
        // TODO check the point quad tree representation in the paper to avoid iterating over all the solutions
        for (int[] sol : paretoFront) {
            if (dominates(dominatingPoint, sol)) {
                int currentPoint = sol[i] - 1;
                if (lowestUpperBound > currentPoint) {
                    lowestUpperBound = currentPoint;
                }
            }
        }
        if (lowestUpperBound < Integer.MAX_VALUE) {
            objectives[i].updateUpperBound(lowestUpperBound, this);
        }
    }

    /**
     * Compute dominated point for objective i,
     * i.e. DP_i = (obj_1_max,...,obj_i_min,...,obj_m_max)
     *
     * @param i index of the variable
     * @return dominated point
     */
    private int[] computeDominatingPoint(int i) {
        int[] dp = new int[objectives.length];
        for (int j = 0; j < objectives.length; j++) {
            dp[j] = objectives[j].getLB();
        }
        dp[i] = objectives[i].getUB();
        return dp;
    }

    public void prepareGIAMaximizerFirstSolution() {
        improveSolution = false;
    }

    public void prepareGIAMaximizerForNextSolution(){
        improveSolution = false;
        paretoFront.add(getLastObjectiveVal().clone());
//        paretoTree.insert(getLastObjectiveVal().clone());
    }


    @Override
    public ESat isEntailed() {
        return ESat.TRUE;
//        if (paretoFront.size() == 0 || !improveSolution){
//            return ESat.TRUE;
//        }
//
//        boolean undefined = false;
//        for (int i = 0; i < n; i++) {
//            if (objectives[i].getUB() < lastObjectiveVal[i]) {
//                if (portfolio){
//                    return ESat.TRUE;
//                }
//                System.out.println("UB: " + objectives[i].getUB() + " < " + lastObjectiveVal[i]);
//                return ESat.FALSE;
//            }
//        }
//        for (int i = 0; i < n; i++) {
//            if (objectives[i].getLB() <= lastObjectiveVal[i]) {
//                undefined = true;
//                break;
//            }
//        }
//        if (undefined) {
//            return ESat.UNDEFINED;
//        }else {
//            return ESat.TRUE;
//        }
    }

    public void setLastObjectiveVal(int[] lastObjectiveVal) {
        this.lastObjectiveVal = lastObjectiveVal;
    }
}

//class KDNode {
//    int[] point; // The point stored in this node
//    KDNode left, right; // Left and right child nodes
//    int axis; // Dimension used to split the space
//
//    KDNode(int[] point, int axis) {
//        this.point = point;
//        this.axis = axis;
//        this.left = null;
//        this.right = null;
//    }
//}

//class KDTree {
//    private KDNode root;
//    private final int dimensions;
//
//    // Constructor
//    public KDTree(int dimensions) {
//        this.dimensions = dimensions;
//        this.root = null;
//    }
//
//    // Insert a point into the KD-Tree
//    public void insert(int[] point) {
//        root = insertRec(root, point, 0);
//    }
//
//    private KDNode insertRec(KDNode node, int[] point, int depth) {
//        if (node == null) {
//            return new KDNode(point, depth % dimensions);
//        }
//
//        int axis = depth % dimensions;
//        if (point[axis] < node.point[axis]) {
//            node.left = insertRec(node.left, point, depth + 1);
//        } else {
//            node.right = insertRec(node.right, point, depth + 1);
//        }
//
//        return node;
//    }
//
//    // Check if a point is dominated and find boundary values
//    public boolean isDominated(int[] point, int[] minBoundary) {
//        return isDominatedRec(root, point, 0, minBoundary);
//    }
//
//    private boolean isDominatedRec(KDNode node, int[] point, int depth, int[] minBoundary) {
//        if (node == null) {
//            return false;
//        }
//
//        // Check if the current node's point dominates the given point
//        if (dominates(node.point, point)) {
//            // Update minBoundary for each objective
//            for (int i = 0; i < dimensions; i++) {
//                if (node.point[i] > minBoundary[i]) {
//                    minBoundary[i] = node.point[i];
//                }
//            }
//            return true;
//        }
//
//        int axis = depth % dimensions;
//
//        // Traverse left or right based on the splitting axis
//        boolean dominated = false;
//        if (point[axis] < node.point[axis]) {
//            dominated = isDominatedRec(node.left, point, depth + 1, minBoundary);
//        } else {
//            dominated = isDominatedRec(node.right, point, depth + 1, minBoundary);
//        }
//
//        // Explore the other subtree if necessary
//        if (!dominated && Math.abs(point[axis] - node.point[axis]) <= 0) {
//            dominated = isDominatedRec(
//                    point[axis] < node.point[axis] ? node.right : node.left,
//                    point, depth + 1, minBoundary
//            );
//        }
//
//        return dominated;
//    }
//
//    // Check if point a dominates point b
//    private boolean dominates(int[] a, int[] b) {
//        boolean strictlyBetter = false;
//        for (int i = 0; i < dimensions; i++) {
//            if (a[i] < b[i]) {
//                return false; // Not dominating
//            }
//            if (a[i] > b[i]) {
//                strictlyBetter = true;
//            }
//        }
//        return strictlyBetter;
//    }
//}

