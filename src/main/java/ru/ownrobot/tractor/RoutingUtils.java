package ru.ownrobot.tractor;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.routing.*;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laboshinl on 9/5/16.
 */
public class RoutingUtils /*extends AbstractActor*/ {
    ActorSystem system = null;
    List<Member>activeMembers = new ArrayList<>();
    Router downloaderRouter = null;
    Router bytesenderRouter = null;
    Router chunksaverRouter = null;

    public Router getChunkdeleterRouter() {
        return chunkdeleterRouter;
    }

    Router chunkdeleterRouter = null;
    Router mapperRouter = null;

    public Router getMapperRouter() {
        return mapperRouter;
    }

    public Router getChunksaverRouter() {
        return chunksaverRouter;
    }

    public Router getBytesenderRouter() {
        return bytesenderRouter;
    }

    public Router getDownloaderRouter() {
        return downloaderRouter;
    }

    public RoutingUtils(ActorSystem system) {
        this.system = system;
        updateMembers();
        this.downloaderRouter = createDownloaderRouter();
        this.bytesenderRouter = createByteSenderRouter();
        this.mapperRouter = createMapperRouter();
        this.chunksaverRouter = createChunkSaverRouter();
        this.chunkdeleterRouter = createChunkDeleterRouter();
    }

    void updateMembers() {

        this.activeMembers.clear();
        Cluster.get(system).state().getMembers().forEach(m -> {
//            if (m.status().equals(MemberStatus.up())) {
            this.activeMembers.add(m);
//            }
        });
        //System.out.println(String.format("Address changed %s", activeMembers.size()));
        this.downloaderRouter = createDownloaderRouter();
        this.bytesenderRouter = createByteSenderRouter();
        this.mapperRouter = createMapperRouter();
        this.chunksaverRouter = createChunkSaverRouter();
        this.chunkdeleterRouter = createChunkDeleterRouter();

    }

    Integer numWorkers = ConfigFactory.load().getInt("workers.count");

    ActorSelection selectTracker(String jobId){
        List<ActorSelection> trackers = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i < numWorkers; i++) {
                trackers.add(system.actorSelection(String.format("%s/user/tracker%s", m.address(), i)));
            }
        });
        return trackers.get(Math.abs(jobId.hashCode()) % trackers.size());
    }

    Router createMapperRouter(){
        List<Routee> mappers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            mappers.add(new ActorSelectionRoutee(system.actorSelection(String.format("/user/mapper%s", i))));
        }
        return  new Router(new RandomRoutingLogic(), mappers);
    }

    Router createByteSenderRouter(){
        List<Routee> mappers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            mappers.add(new ActorSelectionRoutee(system.actorSelection(String.format("/user/bytesender%s", i))));
        }
        return  new Router(new RandomRoutingLogic(), mappers);
    }

    ActorSelection selectReducer(String jobId){
        List<ActorSelection> jobTrackers = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i<numWorkers; i++) {
                jobTrackers.add(system.actorSelection(String.format("%s/user/reducer%s", m.address(), i)));
            }
        });
        return jobTrackers.get(Math.abs(jobId.hashCode()) % jobTrackers.size());
    }

    Router createDownloaderRouter(){
        List<Routee> mappers = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i < numWorkers; i++) {
                mappers.add(new ActorSelectionRoutee(system.actorSelection(String.format("%s/user/chunkdownloader%s", m.address(), i))));
            }
        });
        return  new Router(new RandomRoutingLogic(), mappers);
    }

    Router createChunkSaverRouter(){
        List<Routee> mappers = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i < numWorkers; i++) {
                mappers.add(new ActorSelectionRoutee(system.actorSelection(String.format("%s/user/chunksaver%s", m.address(), i))));
            }
        });
        return  new Router(new RandomRoutingLogic(), mappers);
    }

    Router createChunkDeleterRouter(){
        List<Routee> mappers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            mappers.add(new ActorSelectionRoutee(system.actorSelection(String.format("/user/chunkdeleter%s", i))));
        }
        return  new Router(new RandomRoutingLogic(), mappers);
    }

    ActorSelection selectAggregator(String jobId){
        List<ActorSelection> jobTrackers = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i<numWorkers; i++) {
                jobTrackers.add(system.actorSelection(String.format("%s/user/aggregator%s", m.address(), i)));
            }
        });
        return jobTrackers.get(Math.abs(jobId.hashCode()) % jobTrackers.size());
    }

    List<ActorSelection> getAggregators(){
        List<ActorSelection> aggregators = new ArrayList<>();
        activeMembers.forEach(m -> {
            for (int i = 0; i<numWorkers; i++) {
                aggregators.add(system.actorSelection(String.format("%s/user/aggregator%s", m.address(), i)));
            }
        });
        return aggregators;
    }
}
