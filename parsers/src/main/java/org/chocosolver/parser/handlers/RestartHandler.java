/*
 * This file is part of choco-parsers, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.parser.handlers;

import org.chocosolver.solver.search.strategy.SearchParams;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 15/06/2020
 */
public class RestartHandler extends OneArgumentOptionHandler<SearchParams.ResConf> {

    public RestartHandler(CmdLineParser parser, OptionDef option, Setter<? super SearchParams.ResConf> setter) {
        super(parser, option, setter);
    }

    /**
     * Returns {@code "STRING[]"}.
     *
     * @return return "STRING[]";
     */
    @Override
    public String getDefaultMetaVariable() {
        return "[String,int,double?,int,boolean]";
    }


    @Override
    protected SearchParams.ResConf parse(String argument) throws NumberFormatException, CmdLineException {
        if (argument.startsWith("[")) argument = argument.substring(1);
        if (argument.endsWith("]")) argument = argument.substring(0, argument.length() - 1);
        String[] pars = argument.split(",");
        switch (pars.length) {
            case 4:
                return new SearchParams.ResConf(
                        SearchParams.Restart.valueOf(pars[0].toUpperCase()),
                        Integer.parseInt(pars[1]),
                        Integer.parseInt(pars[2]),
                        Boolean.parseBoolean(pars[3])
                );
            case 5:
                return new SearchParams.ResConf(
                        SearchParams.Restart.valueOf(pars[0].toUpperCase()),
                        Integer.parseInt(pars[1]),
                        Double.parseDouble(pars[2]),
                        Integer.parseInt(pars[3]),
                        Boolean.parseBoolean(pars[4])
                );
            default:
                throw new CmdLineException(owner,
                        Messages.ILLEGAL_UUID, argument);
        }
    }
}
