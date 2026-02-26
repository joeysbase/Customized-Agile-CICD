package fteam.cli_client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "run", description = "run a pipeline file on a specified version of files in a git repo", mixinStandardHelpOptions = true)
public class RunSubcommand implements Callable<Integer> {

    @Option(names = {
            "--name" }, description = "Specify the name of a pipeline file which is under the .pipeline/ directory.", defaultValue = "", paramLabel = "NAME")
    String name;

    @Option(names = {
            "--file" }, description = "Specify the path to a pipeline file.", defaultValue = "", paramLabel = "FILE")
    String file;

    @Option(names = {
            "--commit" }, description = "Specify the commit hash of a file version on which a pipeline file will be executed. If not specified, default to the latest", defaultValue = "latest")
    String commit;

    @Option(names = {
            "--branch" }, description = "Specify the branch of git repo on which a pipeline file will be executed. If not specified, default to main", defaultValue = "main")
    String branch;

    @Override
    public Integer call() {
        if (name.isEmpty() && file.isEmpty()) {
            System.err.println("Error: A pipeline file must be provided.");
            return 1;
        }
        if (!name.isEmpty() && !file.isEmpty()) {
            System.err.println("Error: A pipeline file must be provided.");
            return 1;
        }
        String content;
        if (!name.isEmpty()) {
            try {
                Path path = Path.of("./.pipeline/" + name + ".yaml");
                content = Files.readString(path);
            } catch (IOException e) {
                System.err.println("Error: " + name + ".yaml does not exits or can be read.");
                return 1;
            }
        } else {
            try {
                Path path = Path.of(file);
                content = Files.readString(path);
            } catch (IOException e) {
                System.err.println("Error: " + file + " does not exits or can be read.");
                return 1;
            }
        }
        RequestAgent.requestRunWorker(content, branch, commit);
        for (String s : RequestAgent.MESSAGES) {
            System.out.println(s);
        }
        return 0;
    }

}
