package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.mocoframework.StrategyParams;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;

public class DisjunctiveUpdate implements UpdateRegionsStrategy{

    private List<DisjunctiveRegion> feasibleRegions;
    final private Set<DisjunctiveRegion> infeasibleRegions;
    private DisjunctiveRegion exploredRegion;
    private IntVar[] objectives;

    public DisjunctiveUpdate() {
        feasibleRegions = new ArrayList<>();
        infeasibleRegions = new HashSet<>();
    }

    @Override
    public void update(Set<Region> regions, ParetoArchive archive, IntVar[] objectives, Solution solution, StrategyParams params) {

        if (exploredRegion == null && solution == null) {
            regions.clear();
            return;
        }

        if (this.objectives == null) {
            this.objectives = objectives;
            int[] initialRegion = new int[objectives.length];
            for (int i = 0; i < objectives.length; i++) {
                initialRegion[i] = params.getNadirPoint()[i] - 1;
            }
            feasibleRegions.add(new DisjunctiveRegion(initialRegion, params));
        }

        updateRegions(solution, params);
        if (feasibleRegions.isEmpty()) {
            regions.clear();
        } else {
            selectRegionToExplore(regions, params);
        }
    }

    private void updateRegions(Solution solution, StrategyParams params){
        if (solution == null) {
            infeasibleRegions.add(exploredRegion);
            feasibleRegions.remove(exploredRegion);
        } else {
            feasibleRegions = getAllNewRegions(feasibleRegions, solution, params);
            filterDominatingRegions(feasibleRegions);
            discardInfeasibleRegions(params);
        }
    }

    private List<DisjunctiveRegion> getAllNewRegions(List<DisjunctiveRegion> feasibleRegions, Solution solution, StrategyParams params) {
        List<DisjunctiveRegion> newRegions = new ArrayList<>();
        int[] objectivesValues = solutionToObjectivesValues(solution);
        for (DisjunctiveRegion region: feasibleRegions){
            newRegions.addAll(combineRegionWithSolution(region.v, objectivesValues, params));
        }
        return newRegions;
    }

    private List<DisjunctiveRegion> combineRegionWithSolution(int[] region, int[] objectivesValues, StrategyParams params) {
        List<DisjunctiveRegion> newRegions = new ArrayList<>();
        for (int i = 0; i < objectives.length; i++) {
            int[] newRegion = new int[region.length];
            System.arraycopy(region,0, newRegion, 0, region.length);
            newRegion[i] = Math.max(objectivesValues[i], region[i]);
            newRegions.add(new DisjunctiveRegion(newRegion, params));
        }
        return newRegions;
    }

    private void filterDominatingRegions(List<DisjunctiveRegion> feasibleRegions) {
        for (int i = 0; i < feasibleRegions.size() - 1; i++) {
            for (int j = i+1; j < feasibleRegions.size(); j++) {
                boolean iDominatesJ = true;
                boolean jDominatesI = true;
                for (int k = 0; k < objectives.length; k++) {
                    if (feasibleRegions.get(i).v[k] > feasibleRegions.get(j).v[k]){
                        iDominatesJ = false;
                    }else if (feasibleRegions.get(i).v[k] < feasibleRegions.get(j).v[k]){
                        jDominatesI = false;
                    }
                    if (!iDominatesJ && !jDominatesI) break;
                }
                if (iDominatesJ) {
                    feasibleRegions.remove(j);
                    j--;
                } else if (jDominatesI) {
                    feasibleRegions.remove(i);
                    i--;
                    break;
                }
            }
        }
    }

    private void discardInfeasibleRegions(StrategyParams params) {
        List<DisjunctiveRegion> toRemoveFromFeasible = new ArrayList<>();
        List<DisjunctiveRegion> toAddToInfeasible = new ArrayList<>();

        for (DisjunctiveRegion region : feasibleRegions) {
            boolean regionViolatesIdeal = false;
            for (int k = 0; k < objectives.length; k++) {
                if (region.v[k] >= params.getIdealPoint()[k]) {
                    regionViolatesIdeal = true;
                    break;
                }
            }
            if (regionViolatesIdeal) {
                toAddToInfeasible.add(region);
                toRemoveFromFeasible.add(region);
                continue;
            }
            for (DisjunctiveRegion infeasibleRegion : infeasibleRegions) {
                boolean infeasibleRegionContainsRegion = true;
                for (int k = 0; k < objectives.length; k++) {
                    if (region.v[k] < infeasibleRegion.v[k]) {
                        infeasibleRegionContainsRegion = false;
                        break;
                    }
                }
                if (infeasibleRegionContainsRegion) {
                    toRemoveFromFeasible.add(region);
                    break;
                }
            }
        }

        feasibleRegions.removeAll(toRemoveFromFeasible);
        infeasibleRegions.addAll(toAddToInfeasible);
    }

    private void selectRegionToExplore(Set<Region> regions, StrategyParams params) {
        int idBestRegion = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        int rSize = feasibleRegions.size();
        int lb, ub;

        for (int i = 0; i < rSize; i++) {
            DisjunctiveRegion region = feasibleRegions.get(i);
            float score = 0f;

            for (int k = 0; k < objectives.length; k++) {
                lb = params.getNadirPoint()[k];
                ub = params.getIdealPoint()[k];
                if (ub > lb) {
                    float slack = (ub - region.v[k]) / (float) (ub - lb);
                    score += slack;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                idBestRegion = i;
            }
        }
        exploredRegion = feasibleRegions.get(idBestRegion);
        updateRegionConstraints(regions.iterator().next(), feasibleRegions.get(idBestRegion));
    }

    private void updateRegionConstraints(Region region, DisjunctiveRegion disjunctiveRegion) {
        Model model = objectives[0].getModel();

        if (region.hasConstraints()) {
            List<Constraint> regionConstraints = region.getConstraints();
            for (int i = 0; i < objectives.length; i++) {
                regionConstraints.set(i, model.arithm(objectives[i], ">", disjunctiveRegion.v[i]));
            }
        } else {
            List<Constraint> constraints = new ArrayList<>();
            for (int i = 0; i < objectives.length; i++) {
                constraints.add(model.arithm(objectives[i], ">", disjunctiveRegion.v[i]));
            }
            region.setConstraints(constraints);
        }
    }

    private int[] solutionToObjectivesValues(Solution solution) {
        int[] objectivesValues = new int[objectives.length];
        if (solution == null) {
            return objectivesValues;
        }
        for (int i = 0; i < objectives.length; i++) {
            objectivesValues[i] = solution.getIntVal(objectives[i]);
        }
        return objectivesValues;
    }
}

final class DisjunctiveRegion {
    final int[] v;
    private final int hash;

    DisjunctiveRegion(int[] values, StrategyParams params) {
        int p = params.getIdealPoint().length;
        this.v = new int[p];
        if (values.length < p) {
            System.arraycopy(values, 0, this.v, 0, values.length);
            for (int i = values.length; i < p; i++) {
                this.v[i] = params.getNadirPoint()[i] - 1;
            }
        } else {
            System.arraycopy(values, 0, this.v, 0, p);
        }
        this.hash = Arrays.hashCode(v);  // cached
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DisjunctiveRegion)) return false;
        DisjunctiveRegion r = (DisjunctiveRegion) o;
        return Arrays.equals(v, r.v);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
