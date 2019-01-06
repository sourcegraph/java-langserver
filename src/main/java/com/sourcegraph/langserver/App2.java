package com.sourcegraph.langserver;


import ch.qos.logback.classic.Level;
import com.sourcegraph.common.Config;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App2 {

    private static final long BYTES_TO_GIGABYTES = 1_000_000_000L;

    private static final Logger log = LoggerFactory.getLogger(App2.class);

    private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    public static void main(String... args) throws Exception {
        new App2().run(args);
    }

    private void run(String... args) {

        Options options = new Options();
        options.addOption(Option.builder("p")
                .argName("number")
                .hasArg()
                .desc("Listen on given port (2088)")
                .longOpt("port")
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
        options.addOption(Option.builder("d")
                .desc("Directory where files should be stored (default: /tmp/java-ls)")
                .longOpt("directory")
                .hasArg()
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command cl arguments
            CommandLine cl = parser.parse(options, args);
            if (cl.hasOption("h")) {
                new HelpFormatter().printHelp("java-langserver", options);
                return;
            }
            int port = 2088;
            if (cl.hasOption("p")) {
                port = Integer.parseInt(cl.getOptionValue("p"));
            }

            String storageDir = cl.getOptionValue("d", "/tmp/java-ls");

            // logging
            ch.qos.logback.classic.Logger mainLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.sourcegraph");
            if (cl.hasOption("l")) {
                mainLogger.setLevel(Level.valueOf(cl.getOptionValue("l")));
            }
            System.out.println("Logging level (com.sourcegraph,root)=(" + mainLogger.getLevel() + "," + rootLogger.getLevel()+")");

            Config.checkEnv();

            Runtime runtime = Runtime.getRuntime();
            log.info(String.format("JVM properties (TotalMemory=%.2fGB, MaxMemory=%.2fGB)",
                    ((double)runtime.totalMemory()/BYTES_TO_GIGABYTES),
                    ((double)runtime.maxMemory()/BYTES_TO_GIGABYTES)));

            LSPWebSocketServer wss = new LSPWebSocketServer(port, storageDir);
            wss.start();

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
