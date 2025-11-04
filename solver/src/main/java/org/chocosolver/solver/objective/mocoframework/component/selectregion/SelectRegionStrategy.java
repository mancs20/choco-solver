package org.chocosolver.solver.objective.mocoframework.component.selectregion;

import org.chocosolver.solver.objective.mocoframework.structure.Region;

import java.util.Set;

public interface SelectRegionStrategy {
    // todo set of sets
    Region select(Set<Region> regions);
}