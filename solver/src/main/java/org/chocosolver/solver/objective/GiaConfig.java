package org.chocosolver.solver.objective;

public class GiaConfig {
    /**
     * Enum for the type of boundedness of the GIA algorithm.
     * DOMINATING_REGION: The solution space is only bounded by the regions dominated by the obtained points.
     * DOMINATING_DOMINATES: Like DOMINATING_REGION but also considers the regions that dominate the obtained points
     * there is an upper and lower bound.
     * LAZY_DOMINATING_DOMINATES: For the first solution (when solving the conjunction of disjunctions) use only DOMINATING_REGION, but after that when an initial point
     * is obtained it applies DOMINATING_DOMINATES.
     */
    public enum BoundedType {
        DOMINATING_REGION, DOMINATING_DOMINATES, LAZY_DOMINATING_DOMINATES
    }

    public enum CriteriaSelection {
        NONE, SPARSITY
    }

    public enum Budget {
        NONE // Extend later with TIME, BACKTRACK, etc.
    }

    private BoundedType bounded;
    private CriteriaSelection criteriaSelection;
    private Budget budget;

    // Constructor
    public GiaConfig(BoundedType bounded, CriteriaSelection criteriaSelection, Budget budget) {
        this.bounded = bounded;
        this.criteriaSelection = criteriaSelection;
        this.budget = budget;
    }

    public GiaConfig() {
        this.bounded = BoundedType.DOMINATING_REGION;
        this.criteriaSelection = CriteriaSelection.NONE;
        this.budget = Budget.NONE;
    }

    // Getters
    public BoundedType getBounded() {
        return bounded;
    }

    public CriteriaSelection getCriteriaSelection() {
        return criteriaSelection;
    }

    public Budget getBudget() {
        return budget;
    }

    // Setters
    public void setBounded(BoundedType bounded) {
        this.bounded = bounded;
    }

    public void setCriteriaSelection(CriteriaSelection criteriaSelection) {
        this.criteriaSelection = criteriaSelection;
    }

    public void setBudget(Budget budget) {
        this.budget = budget;
    }
}
