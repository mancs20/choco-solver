package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Priority;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.*;

public abstract class ParetoMaximizerGIAGeneral extends Propagator<IntVar> implements IMonitorSolution {
    protected final IntVar[] objectives;
    protected final int n;
    protected final Model model;

    protected GiaConfig.BoundedType boundedType;

    protected final boolean portfolio;
    protected boolean findingFirstPoint;
//    protected final List<int[]> paretoFront;
    protected final ParetoQuadtree paretoFront;

    protected Solution lastSolution;
    protected int[] lastObjectiveVal;
    protected int[] highestCurrentUpperBounds;
    protected int[] originalUpperBounds;
    // --------------------------------------------------------


    public ParetoMaximizerGIAGeneral(IntVar[] objectives, Priority priority, boolean reactToFineEvt,
                                     boolean portfolio) {
        super(objectives, priority, reactToFineEvt);
        this.objectives = objectives.clone();
        n = objectives.length;
        model = this.objectives[0].getModel();
        this.portfolio = portfolio;
        this.boundedType = GiaConfig.BoundedType.DOMINATING_REGION;
        lastObjectiveVal = new int[n];
        highestCurrentUpperBounds = new int[n];
        this.findingFirstPoint = true;
        this.paretoFront = new ParetoQuadtree(n);
        originalUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            originalUpperBounds[i] = objectives[i].getUB();
        }
    }

    public void setBoundedType(GiaConfig.BoundedType boundedType) {
        this.boundedType = boundedType;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {

    }

    protected void computeDominatedArea() throws ContradictionException{
        boolean someLBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]){
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }else if (objectives[i].getLB() > lastObjectiveVal[i]){
                someLBBiggerThanLastSolution = true;
            }
        }

        if (!someLBBiggerThanLastSolution){
            //all the lower bounds are equal to the last solution, there should be at least one upper bound that is bigger
            //if not then fails
            boolean atLeastOneUBBiggerThanLastSolution = false;
            int idOfTheUBBiggerThanLastSolution = -1; // -1 means that there is more than one upper bound bigger than the last solution
            // check that current solution is bigger than the last one
            for (int i = 0; i < n; i++) {
                if (objectives[i].getUB() > lastObjectiveVal[i]){
                    if (atLeastOneUBBiggerThanLastSolution){
                        idOfTheUBBiggerThanLastSolution = -1;
                        break;
                    }else{
                        atLeastOneUBBiggerThanLastSolution = true;
                        idOfTheUBBiggerThanLastSolution = i;
                    }
                }
            }
            if (atLeastOneUBBiggerThanLastSolution){
                if (idOfTheUBBiggerThanLastSolution != -1) {
                    // only one objective has bigger upper bound than the last solution
                    objectives[idOfTheUBBiggerThanLastSolution].updateLowerBound(lastObjectiveVal[idOfTheUBBiggerThanLastSolution] + 1, this);
                }
            }else{
                fails();
            }
        } //else{
            // all lower bounds are equal or greater than the last solution with at least one lower bound greater than
            // the last solution. setPassive is not working
//            this.setPassive();
//        }
    }

    protected void computeDominatedAreaSimple() throws ContradictionException {
        boolean someUBBiggerThanLastSolution = false;

        // update all objectives lower bound
        for (int i = 0; i < n; i++) {
            if (objectives[i].getLB() < lastObjectiveVal[i]) {
                // the line below cause contradiction if the lower bound cannot take a value equal or bigger than the last solution
                objectives[i].updateLowerBound(lastObjectiveVal[i], this);
            }
            if (objectives[i].getUB() > lastObjectiveVal[i]) {
                someUBBiggerThanLastSolution = true;
            }
        }

        if (!someUBBiggerThanLastSolution) {
            fails();
        }
    }

    protected void setLowestUpperBound(){
        highestCurrentUpperBounds = new int[n];
        for (int i = 0; i < n; i++) {
            highestCurrentUpperBounds[i] = computeLowestUpperBoundWithLastObjectiveVal(i);
        }
    }

    private int computeLowestUpperBoundWithLastObjectiveVal(int i){
        int[] dominatingPoint = computeDominatingPointLastObjectiveVal(i);
        return computeLowestUBToAvoidDomination(dominatingPoint, i);
    }

    protected int computeLowestUBToAvoidDomination(int[] dominatingPoint, int i) {
        int highestPossibleUpperBound = Integer.MAX_VALUE;
        ParetoNode dominatedNode = null;
        do {
            dominatedNode = paretoFront.dominatesAnyInFront(dominatingPoint, paretoFront.getRoot());
            if (dominatedNode == null) {
                break;
            }
            int currentPoint = dominatedNode.point[i] - 1;
            if (highestPossibleUpperBound > currentPoint) {
                highestPossibleUpperBound = currentPoint;
            }
        }   while (dominatedNode != null);

//        for (int[] sol : paretoFront) {
//            if (dominates(dominatingPoint, sol)) {
//                int currentPoint = sol[i] - 1;
//                if (highestPossibleUpperBound > currentPoint) {
//                    highestPossibleUpperBound = currentPoint;
//                }
//            }
//        }
        return highestPossibleUpperBound;
    }

    private int[] computeDominatingPointLastObjectiveVal(int i) {
        int[] dp = lastObjectiveVal.clone();
        dp[i] = originalUpperBounds[i];
        return dp;
    }

    /**
     * Return an int :
     * 0 if a doesn't dominate b
     * 1 if a dominates b
     *
     * @param a vector
     * @param b vector
     * @return a boolean representing the fact that a dominates b
     */
    protected boolean dominates(int[] a, int[] b) {
        for (int j = 0; j < objectives.length; j++) {
            if (a[j] < b[j]) return false;
        }
        return true;
    }

    @Override
    public ESat isEntailed() {
        return null;
    }

    @Override
    public void onSolution() {

    }

    public abstract void prepareGIAMaximizerForNextSolution();

    public abstract void prepareGIAMaximizerFirstSolution();

    public Solution getLastFeasibleSolution() {
        return lastSolution;
    }

    public void setLastSolution(Solution lastSolution) {
        this.lastSolution = lastSolution;
    }

    public int[] getLastObjectiveVal() {
        return lastObjectiveVal;
    }

//    TODO review, maybe it can be deleted-------------------
    public void setLastObjectiveVal(int[] lastObjectiveVal) {
        this.lastObjectiveVal = lastObjectiveVal;
    }
//    --------------------------------------------------------
}


class ParetoNode {
    int[] point;  // The solution (Pareto-optimal point)
    Map<String, ParetoNode> children; // Child nodes (n-dimensional quadrants)
    int dimensions;

    public ParetoNode(int[] point, int dimensions) {
        this.point = Arrays.copyOf(point, point.length);
        this.children = new HashMap<>();
        this.dimensions = dimensions;
    }
}

class ParetoQuadtree {
    private ParetoNode root;
    private int dimensions;
    private int size; // todo maybe not needed

    public ParetoQuadtree(int dimensions) {
        this.root = null;
        this.dimensions = dimensions;
        this.size = 0;
    }

    // Check if point1 dominates point2 in Pareto sense
    private boolean dominates(int[] point1, int[] point2) {
        boolean atLeastOneBetter = false;
        for (int i = 0; i < dimensions; i++) {
            if (point1[i] < point2[i]) {
                return false; // point1 must be >= point2 in all dimensions
            }
            if (point1[i] > point2[i]) {
                atLeastOneBetter = true;
            }
        }
        return atLeastOneBetter;
    }

    // Recursive insertion of a new Pareto-optimal point
    private ParetoNode insert(ParetoNode node, int[] point) {
        if (node == null) {
            return new ParetoNode(point, dimensions);
        }

        // Determine the correct quadrant in n-dimensional space
        String quadrantKey = getQuadrantKey(point, node.point);

        node.children.putIfAbsent(quadrantKey, new ParetoNode(point, dimensions));
        node.children.put(quadrantKey, insert(node.children.get(quadrantKey), point));

        return node;
    }


    // Generate a unique key for a quadrant in n-dimensional space
    private String getQuadrantKey(int[] newPoint, int[] existingPoint) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < dimensions; i++) {
            key.append(newPoint[i] >= existingPoint[i] ? "1" : "0");
        }
        return key.toString(); // Example: "101" for a 3D point
    }

    // Public method to insert a new point into the Pareto quadtree
    public void add(int[] point) {
        this.root = insert(root, point);
        size++;
    }

    public ParetoNode isDominatedByFront(ParetoNode node, int[] point) {
        return checkDominated(node, point);
    }

    private ParetoNode checkDominated(ParetoNode node, int[] point) {
        if (node == null) return null;

        // If the current node dominates the given point, return true immediately
        if (dominates(node.point, point)) return node;

        // Determine the relevant quadrant key to search in
        String quadrantKey = getQuadrantKey(point, node.point);

        // Check only the relevant child quadrant
        ParetoNode relevantChild = node.children.get(quadrantKey);
        if (relevantChild != null) {
            return checkDominated(relevantChild, point);
        }

        return null;
    }

    public ParetoNode getTightestDominator(ParetoNode node, int[] point, int objectiveIndex) {
        return searchTightestDominator(node, point, objectiveIndex, null);
    }

    private ParetoNode searchTightestDominator(ParetoNode node, int[] point, int objectiveIndex, ParetoNode bestDominator) {
        if (node == null) return bestDominator;

        // If this node dominates the given point, check if it is the best so far
        if (dominates(node.point, point)) {
            if (bestDominator == null || node.point[objectiveIndex] < bestDominator.point[objectiveIndex]) {
                bestDominator = node;
            }
        }

        // Determine the relevant quadrant key to search in
        String quadrantKey = getQuadrantKey(point, node.point);

        // Recursively search only in the relevant quadrant
        ParetoNode relevantChild = node.children.get(quadrantKey);
        if (relevantChild != null) {
            return searchTightestDominator(relevantChild, point, objectiveIndex, bestDominator);
        }

        return bestDominator;
    }

    public ParetoNode dominatesAnyInFront(int[] point, ParetoNode node) {
        return checkDominates(node, point);
    }

    private ParetoNode checkDominates(ParetoNode node, int[] point) {
        if (node == null) return null;

        // If the given point dominates the current node's point, return true
        if (dominates(point, node.point)) return node;

        // Determine which quadrants to search (only where a dominated point might exist)
        String quadrantKey = getQuadrantKey(point, node.point);

        // Search only the relevant quadrant
        ParetoNode relevantChild = node.children.get(quadrantKey);
        if (relevantChild != null) {
            return checkDominates(relevantChild, point);
        }

        return null;
    }




    // Print the tree for debugging
    public void printTree() {
        printTree(root, 0);
    }

    private void printTree(ParetoNode node, int level) {
        if (node == null) return;
        System.out.println(new String(new char[level]).replace("\0", "  ")
                + "Node: " + Arrays.toString(node.point) + " | Children: " + node.children.size());
        for (ParetoNode child : node.children.values()) {
            printTree(child, level + 1);
        }
    }

    public ParetoNode getRoot() {
        return root;
    }
}

