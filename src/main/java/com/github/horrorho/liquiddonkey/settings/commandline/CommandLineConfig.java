/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * values copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.settings.commandline;

import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.config.Config;
import com.github.horrorho.liquiddonkey.settings.props.Props;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandLineConfig.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class CommandLineConfig {

    public static CommandLineConfig getInstance() {
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(CommandLineConfig.class);
    private static final CommandLineConfig instance = new CommandLineConfig();

    CommandLineConfig() {
    }

    public Config fromArgs(String[] args) {
        return fromArgs(args, Property.props());
    }

    public Config fromArgs(String[] args, Props<Property> props) {
        logger.trace("<< fromArgs() < {}", (Object) args);
        try {
            // Add command line args
            CommandLineOptions commandLineOptions = CommandLineOptions.newInstance(props);

            props = CommandLinePropsFactory.getInstance().fromCommandLine(props, commandLineOptions, args);

            if (props.contains(Property.COMMAND_LINE_HELP)) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.setOptionComparator(null);
                helpFormatter.printHelp(props.get(Property.APP_NAME) + " [OPTION]... (<token> | <appleid> <password>) ",
                        commandLineOptions.options());
                return null;
            }

            if (props.contains(Property.COMMAND_LINE_VERSION)) {
                System.out.println(props.get(Property.PROJECT_VERSION));
                return null;
            }

            // Build config
            Config config = Config.newInstance(props);

            logger.trace(">> fromArgs() > {}", config);
            return config;

        } catch (ParseException | IllegalArgumentException | IllegalStateException ex) {
            logger.trace("-- fromArgs() > exception: ", ex);
            System.out.println(ex.getLocalizedMessage());
            System.out.println("Try '--help' for more information.");
            return null;
        }
    }
}