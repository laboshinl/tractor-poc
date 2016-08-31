package ru.ownrobot.tractor;

import akka.NotUsed;
import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.MemberStatus;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import ru.ownrobot.tractor.ProtoMessages.*;

public class HttpServer extends AllDirectives  {
    public static ActorSystem system;// = ActorSystem.create("ClusterSystem", ConfigFactory.load());

    public static void main(String[] args) throws IOException {
        String hostname = args.length == 0 ? "127.0.0.1" : args[0];
        // boot up server using the route as defined below
        Config config = ConfigFactory.parseString(String.format(
                "akka.remote.netty.tcp.bind-port=%1$d \n " +
                        "akka.remote.netty.tcp.port=%1$d \n " +
                        "akka.remote.netty.tcp.hostname=%2$s \n " +
                        "akka.cluster.roles = [worker] \n", 2551, hostname))
                .withFallback(ConfigFactory.load());
        system = ActorSystem.create("ClusterSystem", config);

        Integer numWorkers = config.getInt("workers.count");

        system.actorOf(Props.create(DatabaseActor.class),"database");
        system.actorOf(Props.create(JobTrackActor.class), "jobTracker");

        for (int i=0; i< numWorkers; i++) {
            system.actorOf(Props.create(SendBytesActor.class),"bytes" + i);
            system.actorOf(Props.create(ChunkSaveActor.class), "filesystem" + i);
            system.actorOf(Props.create(MapActor.class), "worker" + i);
            system.actorOf(Props.create(AggregateActor.class), "aggregator" + i);
            system.actorOf(Props.create(ChunkDeleteActor.class), "chunkdelete" + i);
            system.actorOf(Props.create(FileDownloadActor.class), "download" + i);
        }


        // HttpApp.bindRoute expects a route being provided by HttpApp.createRoute
        final HttpServer app = new HttpServer();

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("0.0.0.0", 4040), materializer);

//        binding
//                .thenCompose(ServerBinding::unbind)
//                .thenAccept(unbound -> system.terminate());
    }

    public ActorSelection selectNode(String service){
        Cluster cluster = Cluster.get(system);
        final List<ActorSelection> nodes = new ArrayList<>();
        cluster.state().getMembers().forEach(m -> {
            if (m.hasRole("worker") && m.status()== MemberStatus.up())
                nodes.add(system.actorSelection(m.address() + "/user/" + service));
        });
        return nodes.get(0);
    }

    public HashMap clusterStatus(){
        Cluster cluster = Cluster.get(system);
        HashMap <String, String> nodes = new HashMap<>();

        cluster.state().getMembers().forEach(m -> {
                nodes.put(m.address().toString(), m.status().toString());
        });
        return nodes;
    }

    public HashMap<String, Integer>  fileList() {
        HashMap<String, Integer> filelist = new HashMap<>();
        try {

            filelist = (HashMap<String, Integer>) Await.result(Patterns.ask(system.actorSelection("/user/database"), FileListRequest.newBuilder().build(), 10000), Duration.apply("10 sec"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filelist;
    }
    public HashMap<String, Integer>  jobList() {
        HashMap<String, Integer> filelist = new HashMap<>();
        try {
            filelist = (HashMap<String, Integer>) Await.result(Patterns.ask(system.actorSelection("/user/database"), JobListRequest.newBuilder().build(), 10000), Duration.apply("10 sec"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filelist;
    }

    public HashMap<String, String>  optionsList() {
        Config config = ConfigFactory.load();
        HashMap<String, String> options = new HashMap<>();
        options.put("Chunk size", String.valueOf(config.getInt("filesystem.chunksize") + " MB"));
        options.put("Worker count", String.valueOf(config.getInt("workers.count")));
        return options;
    }

    public Integer  deleteFile(String fileName) {
        Integer chunks = 0;
        try {
            chunks = (Integer) Await.result(Patterns.ask(system.actorSelection("/user/database"), FileDeleteRequest.newBuilder().setFileName(fileName).build(), 10000), Duration.apply("10 sec"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chunks;
    }

    public String renderTemplate(Object data, String action, String delete, String nextlink){
        Writer writer = new StringWriter();
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        VelocityContext context = new VelocityContext();
        context.put("action", action);
        context.put("delete", delete);
        context.put("data", data);
        context.put("nextlink", nextlink);

        if(data instanceof HashMap) {
            Template t = ve.getTemplate("hashmap.vm");
            t.merge(context, writer);

        } else if(data instanceof String) {
            Template t = ve.getTemplate("string.vm");
            t.merge(context, writer);
        }
        return writer.toString();
    }

    public Route createRoute() {
        // This handler generates responses to `/hello?name=XXX` requests
        Route processRoute =
                parameterOptional("name", optName -> {
                    String fileName = optName.orElse("none");
                    system.actorSelection("/user/database").tell(FileProcessRequest.newBuilder().setFileName(fileName).build(), ActorRef.noSender());
                    return complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(
                            String.format("<div class=\"alert alert-info\" role=\"alert\">Started job <strong> %s </strong> for file %s </div>", "???", fileName), null, null,"jobs")));
                });
        Route fileDownloadRoute  =
                parameterOptional("name", optName -> {
                    String url = optName.orElse("none");
                    system.actorSelection("/user/download0").tell(ProtoMessages.FileDownloadRequest.newBuilder().setUrl(url).build(), ActorRef.noSender() );
                    return complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(
                            String.format("<div class=\"alert alert-info\" role=\"alert\">Uploading file <strong> %s </strong> </div>", url), null, null,"files")));
                });
        Route fileDeleteRoute =
                parameterOptional("name", optName -> {
                    String filename = optName.orElse("none");
                    Integer chunks = deleteFile(filename);
                    return complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(
                            String.format("<div class=\"alert alert-info\" role=\"alert\">Deleting file <strong> %s </strong> (%s) chunks </div>", filename, chunks),null,null,"files")));
                });
        Route jobDeleteRoute =
                parameterOptional("name", optName -> {
                    String jobId = optName.orElse("none");
                    system.actorSelection("/user/database").tell(JobDeleteRequest.newBuilder().setJobId(jobId).build(), ActorRef.noSender());
                    return complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(
                            String.format("<div class=\"alert alert-info\" role=\"alert\">Deleted job <strong> %s </strong> </div>", jobId),null,null,"jobs")));

                });
        return
                // here the complete behavior for this server is defined

                // only handle GET requests
                get(() -> route(
                        // matches the empty path
                        pathSingleSlash(() ->
                                        complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(clusterStatus(), null, null,null)))
                        ),
                        path("files", () ->
                                        // return a simple `text/plain` response
                                        complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(fileList(), "process", "filedelete", null)))
                        ),
                        path("jobs", () ->
                                        // return a simple `text/plain` response
                                        complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(jobList(), null, "jobdelete",null)))
                        ),

                        path("process", () ->
                                        // uses the route defined above
                                        processRoute
                        ),
                        path("config", () ->
                                        // uses the route defined above
                                        complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(optionsList(), null, null ,null)))
                        ),
                        path("filedelete", () ->
                                        // uses the route defined above
                                        fileDeleteRoute
                        ),
                        path("download", () ->
                                        // uses the route defined above
                                        fileDownloadRoute
                        ),
                        path("jobdelete", () ->
                                        // uses the route defined above
                                        jobDeleteRoute
                        ),
                        path("status", () ->
                                        // return a simple `text/plain` response
                                        complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, renderTemplate(clusterStatus(), null, null,null)))
                        )

                ));
    }
}

