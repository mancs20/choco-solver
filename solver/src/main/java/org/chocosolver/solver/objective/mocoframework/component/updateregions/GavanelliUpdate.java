package org.chocosolver.solver.objective.mocoframework.component.updateregions;

import org.chocosolver.solver.Solution;
import org.chocosolver.solver.objective.mocoframework.structure.ParetoArchive;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.Map;
import java.util.Set;

public class GavanelliUpdate implements UpdateRegionsStrategy{
    @Override
    public void update(Set<Region> regions, ParetoArchive archive, IntVar[] objectives, Solution solution, Map<String, Object> params) {
        if (solution == null) {
            regions.clear();
        }
    }
}
