package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;

import java.util.*;


/**
 * Created by laboshinl on 8/4/16.
 */
public class AggregateActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    ActorSystem system = getContext().system();

    HashMap<String, Integer> finishedJobs = new HashMap<>();
    HashMap<Long, FlowStat> flows = new HashMap<>();

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof WorkerMsgs.PacketMsg){
            WorkerMsgs.PacketMsg packetMsg = (WorkerMsgs.PacketMsg) message;
            FlowStat flow = flows.containsKey(packetMsg.flowHash) ? flows.get(packetMsg.flowHash) : new FlowStat();
            flow.add(packetMsg.packet);
            flows.put(packetMsg.flowHash, flow);
        }
        else if (message instanceof WorkerMsgs.JobStatus){
            WorkerMsgs.JobStatus status = (WorkerMsgs.JobStatus) message;

            Integer count = finishedJobs.containsKey(status.jobId) ? finishedJobs.get(status.jobId) : 0;
            finishedJobs.put(status.jobId, count + 1);

            System.out.println(String.format("%s/%s jobs finished ID=%s", count + 1, status.numProcessed, status.jobId));

            if (finishedJobs.get(status.jobId).equals(status.numProcessed)) {

                flows.entrySet().stream()
                        .sorted(Map.Entry.<Long, FlowStat>comparingByValue().reversed())
                        .limit(10)
                        .forEach(v -> {System.out.println(v.getValue());});

                //flows.forEach((k,v) -> {System.out.println(String.format("%s:%s %s:%s total %s", v.clientIp, v.clientPort, v.serverIp, v.serverPort, v.totalCount));});

                flows.clear();
                finishedJobs.remove(status.jobId);
                system.actorFor("/user/database").tell(new WorkerMsgs.JobStatus(status.jobId, 100), self());

            }
            else
                system.actorFor("/user/database").tell(new WorkerMsgs.JobStatus(status.jobId, finishedJobs.get(status.jobId)*100/status.numProcessed), self());


        } else {
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Aggregate actor started");
    }
    public void postStop(){
        log.info("Aggregate actor stopped");
    }
}
