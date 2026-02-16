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

    static void setDefaultSearchMultiObjective(Model model, IntVar[] objectives, IntVar[] decisionVars, String searchStrategy) {
        if (model.getSolver().getSearch() == null) {
            if (decisionVars.length == 0) {
                IntVar[] notObjectivesVars = getNonObjectiveVariables(model, objectives);
                setSearchMultiObjective(searchStrategy, model, notObjectivesVars);
            } else {
                setSearchMultiObjective(searchStrategy, model, decisionVars);
            }

            //todo test
            // objectives first
//            IntVar[] vars = new IntVar[objectives.length + decisionVars.length];
//            System.arraycopy(objectives, 0, vars, 0, objectives.length);
//            System.arraycopy(decisionVars, 0, vars, objectives.length, decisionVars.length);
            //keep trying this one, saving the weights during gavanelli stage
//            model.getSolver().setSearch(Search.domOverWDegSearch(vars));
        }
    }

    static void setSearchMultiObjective(String searchStrategy, Model model, IntVar[] decisionVars) {
        if ("minDomLBSearch".equals(searchStrategy)) {
            model.getSolver().setSearch(Search.minDomLBSearch(decisionVars));
        } else {
            model.getSolver().setSearch(Search.domOverWDegSearch(decisionVars));
        }

    }
}
