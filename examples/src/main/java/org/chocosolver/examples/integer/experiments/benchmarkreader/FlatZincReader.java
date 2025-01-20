package org.chocosolver.examples.integer.experiments.benchmarkreader;

import org.chocosolver.examples.integer.experiments.Config;
import org.chocosolver.parser.SetUpException;
import org.chocosolver.parser.flatzinc.Flatzinc;
import org.chocosolver.parser.flatzinc.ast.Datas;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import static java.lang.System.exit;

public class FlatZincReader extends BenchamarkReader{

    public FlatZincReader(Config config) {
        super(config);
    }

    @Override
    public ModelObjectivesVariables createModel(int index) {
        String filePath = config.getInstancePath();
        ModelObjectivesVariables mov = null;
        // read the flatzinc file
        Flatzinc fzn = new Flatzinc();
        try {
            if(fzn.setUp(filePath)) {
                fzn.createSettings();
                fzn.createSolver();
                fzn.buildModel();
                Model model = fzn.getModel();
                Datas[] datas = fzn.datas;
                IntVar[] objectives = (IntVar[]) datas[0].get("objs");
                if (objectives == null) {
                    System.out.println("There are no objectives, you need to group them in an array " +
                            "called objs in the minizinc model file");
                    System.exit(1);
                }
                Object[] decisionVariables = null;
                if(datas[0].get("decision_vars") != null) {
                    if (datas[0].get("decision_vars").getClass() == IntVar[].class ||
                            datas[0].get("decision_vars").getClass() == BoolVar[].class) {
                        decisionVariables = (IntVar[]) datas[0].get("decision_vars");
                    }
                }else if(datas[0].get("DECISION_VARS") != null) {
                    decisionVariables = (IntVar[]) datas[0].get("DECISION_VARS");
                }else{
                    System.out.println("If you want to access the decision variables, you need to group them in an array or set " +
                            "called decision_vars or DECISION_VARS in the minizinc model file");
                }

                boolean maximization;
                if (datas[0].get("maximization") != null && datas[0].get("minimization") == null) {
                    maximization = true;
                }else if(datas[0].get("minimization") != null && datas[0].get("maximization") == null) {
                    maximization = false;
                }else {
                    maximization = false;
                    System.out.println("You need to specify if the problem is a maximization or minimization problem by adding a variable " +
                            "called maximization or minimization in the minizinc model file. Check that only one of them is present.");
                    System.exit(1);
                }
                setMaxMinTrue(model, datas);
                mov = new ModelObjectivesVariables(model, objectives, decisionVariables, maximization);
                return mov;
            }else{
                System.out.println("Error");
                exit(1);
            }
        } catch (SetUpException e) {
            e.printStackTrace();
            exit(1);
        }
        return mov;
    }

    private void setMaxMinTrue(Model model, Datas[] datas) {
        Variable[] outPutVars = datas[0].allOutPutVars();
        for (int i = 0; i < outPutVars.length; i++) {
            if (outPutVars[i].getName().equals("maximization") || outPutVars[i].getName().equals("minimization")) {
                model.arithm(outPutVars[i].asIntVar(), "=", 1).post();
                break;
            }
        }
    }
}
