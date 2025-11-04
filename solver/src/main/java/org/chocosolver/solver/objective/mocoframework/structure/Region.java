package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.constraints.Constraint;

import java.util.Set;

public class Region {
    private final Set<Constraint> constraints;
    private boolean dummy = false;

    public Region() {
        this.dummy = true;
        this.constraints = null;
    }

    public Region(Set<Constraint> constraints) {
        this.constraints = constraints;
    }

    public Set<Constraint> getConstraints() {
        return constraints;
    }

    public boolean isDummy() {
        return dummy;
    }

//    @Override
//    public String toString() {
//        return constraints.toString();
//    }
}
