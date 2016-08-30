package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import ru.ownrobot.tractor.KryoMessages.JobProgress;
import ru.ownrobot.tractor.ProtoMessages.JobFinishedMsg;
import ru.ownrobot.tractor.ProtoMessages.JobStatusMsg;
import ru.ownrobot.tractor.ProtoMessages.NewJobMsg;

import java.util.HashMap;

public class JobTrackActor extends UntypedActor {
    private final HashMap<String, JobProgress> jobs = new HashMap<>();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof NewJobMsg) {
            NewJobMsg newJob = (NewJobMsg) message;
            jobs.put(newJob.getJobId(), new JobProgress(newJob.getCount()));
        }
        if (message instanceof JobStatusMsg) {
            JobStatusMsg jobStatus = (JobStatusMsg) message;
            if(jobs.containsKey(jobStatus.getJobId())) {
                JobProgress progress = jobs.get(jobStatus.getJobId());
                progress.addWorker(getSender());
                progress.increment();
                log.info("Job {} {} % complete", jobStatus.getJobId(), progress.getProgress());
                if (progress.isFinished()) {
                    progress.getWorkers()
                            .forEach(i -> i.tell(JobFinishedMsg.newBuilder().setJobId(jobStatus.getJobId()).build(), self()));
                    jobs.remove(jobStatus.getJobId());
                } else
                    jobs.put(jobStatus.getJobId(), progress);
            }
            else
                log.error("No such job {}", jobStatus.getJobId());
        }
        else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Tracker actor started");
    }
    public void postStop(){
        log.info("Tracker actor stopped");
    }

}
