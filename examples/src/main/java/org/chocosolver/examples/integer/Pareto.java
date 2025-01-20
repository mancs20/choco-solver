package org.chocosolver.examples.integer;/*
 * Copyright (C) 2017 COSLING S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Choco Solver example involving multi-objective optimization
 * @author Jean-Guillaume FAGES (cosling)
 * @version choco-solver-4.0.4
 */
public class Pareto {

    public Object[] run(Model model, IntVar[] objectives, boolean maximize) {
        // Optimise independently two variables using the Pareto optimizer
        long startTime = System.nanoTime();
        List<Solution> solutions = model.getSolver().findParetoFront(objectives, maximize);
        long endTime = System.nanoTime();
        float elapsedTime = (float) (endTime - startTime) / 1_000_000_000;
        // stats
        List<String> recorderList = new ArrayList<>();
        recorderList.add(model.getSolver().getMeasures().toString());

        return new Object[]{solutions, recorderList, elapsedTime};
    }
}