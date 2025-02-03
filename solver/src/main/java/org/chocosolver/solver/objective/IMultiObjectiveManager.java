package org.chocosolver.solver.objective;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

public interface IMultiObjectiveManager {

    static IntVar[] getNonObjectiveVariables(Model model, IntVar[] objectives) {
        IntVar[] tempModelVars = model.retrieveIntVars(true);
        IntVar[] notObjectivesVars = new IntVar[tempModelVars.length - objectives.length];
        int index = 0;
        boolean notObjective;
        for (int i = 0; i < notObjectivesVars.length; i++) {
            notObjective = true;
            for (IntVar objective : objectives) {
                if (tempModelVars[i] == objective) {
                    notObjective = false;
                    break;
                }
            }
            if (notObjective) {
                notObjectivesVars[index] = tempModelVars[i];
                index++;
            }

        }
        return notObjectivesVars;
    }

    static void setDefaultSearchMultiObjective(Model model, IntVar[] objectives, IntVar[] decisionVars) {
        if (model.getSolver().getSearch() == null) {
            if (decisionVars.length == 0) {
                IntVar[] notObjectivesVars = getNonObjectiveVariables(model, objectives);
                model.getSolver().setSearch(Search.domOverWDegRefSearch(notObjectivesVars),
                        Search.domOverWDegRefSearch(objectives));
            } else {
                model.getSolver().setSearch(Search.domOverWDegRefSearch(decisionVars),
                        Search.domOverWDegRefSearch(objectives));

            }
        }
    }
}
