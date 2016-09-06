package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ru.ownrobot.tractor.ProtoMessages.*;
import ru.ownrobot.tractor.KryoMessages.*;

import java.util.*;


public class AggregateActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final HashMap<String, HashMap<Long, FlowStat>> jobs = new HashMap<>();
    private final RoutingUtils routers = new RoutingUtils(getContext().system());

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof TractorPacketMsg){
            TractorPacketMsg tractorPacketMsg = (TractorPacketMsg) message;
            HashMap<Long, FlowStat> flows = jobs.containsKey(tractorPacketMsg.getJobId())? jobs.get(tractorPacketMsg.getJobId()) : new HashMap<>();
            FlowStat flow = flows.containsKey(tractorPacketMsg.getFlowId()) ? flows.get(tractorPacketMsg.getFlowId()) : new FlowStat();
            flow.add(tractorPacketMsg.getPacket());
            flows.put(tractorPacketMsg.getFlowId(), flow);
            jobs.put(tractorPacketMsg.getJobId(),flows);
        }
        else if (message instanceof JobFinishedMsg){
            String jobId = ((JobFinishedMsg) message).getJobId();
            if(jobs.containsKey(jobId)) {
                routers.selectReducer(jobId).tell(new JobResult(jobId, jobs.get(jobId)), self());
                jobs.remove(jobId);
            } else
                routers.selectReducer(jobId).tell(new JobResult(jobId, jobs.get(jobId)), self());
        } else if (message instanceof ClusterEvent.MemberEvent){
            routers.updateMembers();
        } else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
       Cluster.get(getContext().system()).subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
               ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
        log.info("Aggregate actor started");
    }
    public void postStop(){
        log.info("Aggregate actor stopped");
    }
}
