package org.chocosolver.solver.objective.mocoframework.component.initialregion;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.objective.mocoframework.structure.Region;
import org.chocosolver.solver.variables.IntVar;

import java.util.Set;

public class ParetoGlobalRegion implements InitialRegionStrategy{
    @Override
    public Set<Region> computeInitialRegion(Model model, IntVar[] objectives) {
        return null;
    }
}
