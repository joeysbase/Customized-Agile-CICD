package fteam.cli_client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "dryrun", description = "Validate a pipeline file and output its job execution order if valid", mixinStandardHelpOptions = true)
public class DryrunSubcommand implements Callable<Integer> {

    @Parameters(index = "0", description = "path to the pipeline configuration file to be dryran.", defaultValue = "")
    String filePath;

    @Override
    public Integer call() {
        if (filePath.isEmpty()) {
            System.err
                    .println("Error: Must provide a pipeline file to dryrun. Use cicd dryrun --help to see examples. ");
            return 1;
        }
        Path path = Path.of(filePath);
        if (Files.isDirectory(path)) {
            System.err.println("Error: " + filePath + " is a directory.");
            return 1;
        }
        try {
            String content=Files.readString(path);
            RequestAgent.requestDryrunWorker(content);
            System.out.println("Dryrun results of "+filePath+" are as follow:");
            for(String s:RequestAgent.MESSAGES){
                System.out.println(s);
            }
            return 0;
        } catch (IOException e) {
            System.err.println("Error: Unable to read file "+filePath+" .");
            System.err.println(e);
            return 1;
        }
    }

}
