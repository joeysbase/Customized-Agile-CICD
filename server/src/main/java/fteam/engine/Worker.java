package fteam.engine;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Worker implements Runnable{
    private List<String> messages;
    private AtomicBoolean status;

    public void addMessage(String msg){
        this.messages.add(msg);
    }

    public List<String> getMessages(){
        return messages;
    }

    public void setStatus(AtomicBoolean status){
        this.status=status;
    }

    public void setMessages(List<String> messages){
        this.messages=messages;
    }

    public AtomicBoolean getStatus(){
        return status;
    }

    public void setWorkDone(){
        this.status.set(true);
    }

    public boolean isWorkDone(){
        return status.get();
    }
}
