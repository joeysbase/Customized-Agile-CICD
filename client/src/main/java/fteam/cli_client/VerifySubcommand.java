package fteam.cli_client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "verify",
    description = "Verify if a given cicd configuration file is valid",
    mixinStandardHelpOptions = true)
public class VerifySubcommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "relative path to configuration files",
      defaultValue = "",
      paramLabel = "FILE")
  String filePath;

  @Override
  public Integer call() {
    List<String> results = new ArrayList<>();
    if (filePath.isEmpty()) {
      System.out.println(
          "Warning: No configuration file or folder given, using default value"
              + " \".pipeline/pipeline.yaml\".");
      filePath = ".pipeline/pipeline.yaml";
    }
    Path path = Path.of(filePath);
    if (Files.isDirectory(path)) {
      File dir = path.toFile();
      for (File file : dir.listFiles()) {
        if (file.isFile()
            && (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml"))) {
          StringBuilder sb=new StringBuilder();
          sb.append(file.getPath()).append(": \n");
          try {
            String content=Files.readString(file.toPath());
            System.out.println("Verifying "+file.getName()+" .\n");
            RequestAgent.requestVerifyWorker(content);
            
            for(String msg:RequestAgent.MESSAGES){
              sb.append("\t- ").append(msg).append("\n");
            }
            results.add(sb.toString());
          } catch (IOException e) {
            sb.append(file.getPath()).append(": \n");
            sb.append("Error: Unable to open file ").append("\"").append(file.getName()).append("\".");
            results.add(sb.toString());
          } catch (Exception e) {
            sb.append(file.getPath()).append(": \n");
            sb.append(e);
            results.add(sb.toString());
          }
        }
      }
    } else if (Files.isRegularFile(path)) {
      File file = path.toFile();
      StringBuilder sb=new StringBuilder();
      sb.append(file.getPath()).append(": \n");
      try {
        String content=Files.readString(file.toPath());
        System.out.println("Verifying "+file.getName()+" .\n");
        RequestAgent.requestVerifyWorker(content);
        
            for(String msg:RequestAgent.MESSAGES){
              sb.append("\t- ").append(msg).append("\n");
            }
            results.add(sb.toString());
      } catch (IOException e) {
        sb.append(file.getPath()).append(": \n");
        sb.append("Error: Unable to open file ").append("\"").append(file.getName()).append("\".");
        results.add(sb.toString());
      } catch (Exception e) {
        sb.append(file.getPath()).append(": \n");
        sb.append(e);
        results.add(sb.toString());
      }
    } else {
      System.err.println(
          "Error: The file or directory " + "\"" + filePath + "\"" + " do not exist.");
      return 1;
    }
    System.out.println("Verification results are as follow:\n\n");
    for(String s:results){
      System.out.println(s);
    }
    if(RequestAgent.MESSAGES.isEmpty()){
      System.out.println("OK: pipeline configuration is valid.");
    }
    return 0;
  }
}
