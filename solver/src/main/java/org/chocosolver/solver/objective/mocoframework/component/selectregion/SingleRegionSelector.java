package org.chocosolver.solver.objective.mocoframework.component.selectregion;

import org.chocosolver.solver.objective.mocoframework.structure.Region;

import java.util.Set;

public class SingleRegionSelector implements SelectRegionStrategy{
    @Override
    public Region select(Set<Region> regions) {
        return regions.iterator().next();
    }
}
