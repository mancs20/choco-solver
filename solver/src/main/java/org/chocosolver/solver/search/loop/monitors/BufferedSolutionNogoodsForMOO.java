package org.chocosolver.solver.search.loop.monitors;

import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.constraints.nary.sat.PropSat;
import org.chocosolver.solver.variables.IntVar;
import gnu.trove.list.array.TIntArrayList;

/**
 * Avoid exploring same solutions after a reset (useful in multi-objective optimization)
 * Beware :
 * - Must be plugged as a monitor
 * - Only works for integer variables
 * <p>
 *
 * @author Manuel Combarro Simón
 * @since 04/02/26
 */

public final class BufferedSolutionNogoodsForMOO implements IMonitorSolution, IMonitorInitialize {

    private final PropSat png;
    private final IntVar[] vars;
    private final int n;
    private final TIntArrayList litBuf;

    public BufferedSolutionNogoodsForMOO(IntVar... vars) {
        this.vars = vars;
        this.n = vars.length;
        this.png = vars[0].getModel().getMinisat().getPropSat();
        this.litBuf = new TIntArrayList(64 * n); // just initial capacity
    }

    @Override
    public void onSolution() {
        for (int i = 0; i < n; i++) {
            int v = vars[i].getValue();
            litBuf.add(MiniSat.makeLiteral(png.makeIntEq(vars[i], v), false));
        }
    }

    @Override
    public void beforeInitialize() {
        flush();
    }

    private void flush() {
        int m = litBuf.size();
        for (int off = 0; off < m; off += n) {
            int[] ng = new int[n]; // fresh array => no aliasing
            for (int i = 0; i < n; i++) ng[i] = litBuf.get(off + i);
//            png.addLearnt(ng);
            png.addLearntUsingBuckets(n, ng);
        }
        litBuf.resetQuick();
    }
}


