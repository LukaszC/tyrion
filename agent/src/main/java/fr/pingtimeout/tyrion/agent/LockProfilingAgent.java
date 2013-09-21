/*
 * Copyright (c) 2013, Pierre Laporte
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this work; if not, see <http://www.gnu.org/licenses/>.
 */

package fr.pingtimeout.tyrion.agent;

import fr.pingtimeout.tyrion.transformation.TyrionTransformer;
import fr.pingtimeout.tyrion.util.EventsWriter;
import fr.pingtimeout.tyrion.util.SimpleLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class LockProfilingAgent {
    /**
     * JVM hook to statically load the javaagent at startup.
     * <p/>
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     *
     * @param args The agent's arguments
     * @param inst The instrumentation class that will be used
     * @throws Exception
     */
    public static void premain(String args, Instrumentation inst) throws Exception {
        Configuration configuration = new Configuration(args);
        Configuration.ParameterValue outputFileParameter = configuration.outputFile();

        if (outputFileParameter.isDefaultValue()) {
            SimpleLogger.warn("No output file was provided, agent is disabled");
        } else {
            SimpleLogger.info("Tyrion agent starting with arguments '%s'", configuration);

            final String outputFile = outputFileParameter.getValue();

            clearOutputFile(outputFile);
            scheduleLocksWrite(outputFile);
            configureLockInterceptor(configuration);
            addLocksTransformer(inst);
        }
    }

    private static void clearOutputFile(String outputFile) {
        try (FileOutputStream erasor = new FileOutputStream(outputFile)) {
            erasor.write("".getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            SimpleLogger.warn("Output file could not be cleared. Cause : %s", e.getMessage());
            SimpleLogger.debug(e);
        }
    }


    private static void addLocksTransformer(Instrumentation inst) {
        inst.addTransformer(new LocksTransformer(new TyrionTransformer()));
    }


    private static void scheduleLocksWrite(String outputFile) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, EventsWriter.THREAD_NAME);
                t.setDaemon(true);
                return t;
            }
        });
        executorService.scheduleAtFixedRate(new EventsWriter(outputFile), 1, 1, TimeUnit.SECONDS);
    }


    private static void configureLockInterceptor(Configuration configuration) {
        LockInterceptor lockInterceptor = LockInterceptor.getInstance();

        Configuration.ParameterValue excludedThreadsParam = configuration.excludedThreads();
        for (String excludedThreadName : excludedThreadsParam.getValues()) {
            lockInterceptor.addExcludedThread(excludedThreadName);
        }

        String enabledAtStartup = configuration.isEnabledAtStartup().getValue();
        if (Boolean.valueOf(enabledAtStartup)) {
            lockInterceptor.setEnabled(true);
        }
    }
}
