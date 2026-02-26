package fteam.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VerifyWorker extends Worker{
    private final String ConfigurationString;

    private VerifyWorker(String configStr){
        this.ConfigurationString=configStr;
    }

    public static VerifyWorker fromFile(String path){
        Path filePath=Path.of(path);
        try {
            String content=Files.readString(filePath);
            return new VerifyWorker(content);
        } catch (IOException e) {
            return null;
        }
    }

    public static VerifyWorker fromFileString(String fileString){
        return new VerifyWorker(fileString);
    }


    @Override
    public void run(){
        PipelineConfig config=PipelineConfig.fromFile(ConfigurationString);
        for(String s:config.getVerificationMsg()){
            addMessage(s);
        }
        setWorkDone();
    }
    
}
