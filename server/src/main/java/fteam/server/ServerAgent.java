package fteam.server;

import fteam.server.handler.DryrunHandler;
import fteam.server.handler.RunHandler;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import fteam.server.handler.VerifyHandler;

public class ServerAgent {
  public void start() throws Exception {

    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/verify", new VerifyHandler());
    server.createContext("/dryrun", new DryrunHandler());
    server.createContext("/run", new RunHandler());
    server.start();
    System.out.println("Server started on 8080");
  }
}
