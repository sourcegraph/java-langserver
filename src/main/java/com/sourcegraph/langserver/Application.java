package com.sourcegraph.langserver;

import ch.qos.logback.classic.Level;
import com.sourcegraph.common.Config;
import com.sourcegraph.lsp.Controller;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

public class Application {

    private static final long BYTES_TO_GIGABYTES = 1_000_000_000L;

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    public static void main(String... args) throws Exception {
        new Application().run(args);
    }

    private void run(String... args) throws Exception {

        Options options = new Options();
        options.addOption(Option.builder("p")
                .argName("number")
                .hasArg()
                .desc("Listen on given port (2088)")
                .longOpt("port")
                .build());
        options.addOption(Option.builder("f")
                .desc("Filesystem mode (opposite to default VFS mode)")
                .longOpt("file")
                .build());
        options.addOption(Option.builder("l")
                .argName("level")
                .hasArg()
                .desc("Log level (DEBUG : TRACE | DEBUG | INFO | WARN | ERROR)")
                .longOpt("log")
                .build());
        options.addOption(Option.builder("h")
                .desc("Show help")
                .longOpt("help")
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("java-langserver", options);
                return;
            }
            int port = 2088;
            if (line.hasOption("p")) {
                port = Integer.parseInt(line.getOptionValue("p"));
            }

            // logging
            ch.qos.logback.classic.Logger mainLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.sourcegraph");
            if (line.hasOption("l")) {
                mainLogger.setLevel(Level.valueOf(line.getOptionValue("l")));
            }
            System.out.println("Logging level (com.sourcegraph,root)=(" + mainLogger.getLevel() + "," + rootLogger.getLevel()+")");

            Config.checkEnv();

            Runtime runtime = Runtime.getRuntime();
            log.info(String.format("JVM properties (TotalMemory=%.2fGB, MaxMemory=%.2fGB)",
                    ((double)runtime.totalMemory()/BYTES_TO_GIGABYTES),
                    ((double)runtime.maxMemory()/BYTES_TO_GIGABYTES)));

            JavaLspHandlerServiceFactory factory = new JavaLspHandlerServiceFactory(!line.hasOption("f"));

            Controller.serve(port, factory::newHandlerService, controller -> {
                if (StringUtils.isEmpty(Config.LIGHTSTEP_TOKEN)) {
                    return null;
                }
                try {
                    return new com.lightstep.tracer.jre.JRETracer(
                            new com.lightstep.tracer.shared.Options.OptionsBuilder()
                                    .withAccessToken(Config.LIGHTSTEP_TOKEN)
                                    .withComponentName("java-langserver")
                                    .build());
                } catch (MalformedURLException e) {
                    log.error("An error occurred while initializing Lightstep tracing");
                    e.printStackTrace();
                    return null;
                }
            }, true);

            synchronized (this) {
                while (true) {
                    try {
                        wait();
                    } catch (InterruptedException exception) {
                        log.debug("Waiting for all tasks to finish");
                    }
                }
            }

        } catch (ParseException exp) {
            log.error("Parsing failed.  Reason: ", exp);
            new HelpFormatter().printHelp("java-langserver", options);
            System.exit(1);
        } catch (Config.ConfigException exp) {
            log.error("Configuration error: " + exp);
            System.exit(1);
        }
    }
}
