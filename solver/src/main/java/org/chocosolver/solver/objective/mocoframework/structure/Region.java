package org.chocosolver.solver.objective.mocoframework.structure;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;

import java.util.List;

public class Region {
    private List<Constraint> constraints;

    public Region() {
        this.constraints = null;
    }

    public Region(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public boolean hasConstraints() {
        return constraints != null && !constraints.isEmpty();
    }

    public void unpostConstraints(Model model) {
        if (hasConstraints()) {
            for (Constraint c : constraints) {
                model.unpost(c);
            }
        }
    }

    public void postConstraints() {
        if (hasConstraints()) {
            for (Constraint c : constraints) {
                c.post();
            }
        }
    }
}
