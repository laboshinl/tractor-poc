package ru.ownrobot.tractor;

import akka.util.ByteString;

import java.io.Serializable;

/**
 * Created by laboshinl on 8/2/16.
 */
public class WorkerMsgs {
    public static class FileChunk implements Serializable{
        public ByteString data;
        public String filename;
        public FileChunk(String filename, ByteString data){
            this.filename = filename;
            this.data = data;
        }
    }
    public static class PacketStatus implements Serializable{
        public ByteString data;
        public boolean status;
        public PacketStatus(boolean status, ByteString data){
            this.status = status;
            this.data = data;
        }
    }

    public static class JobStatus implements Serializable {
        public String jobId;
        public Integer numProcessed;
        public JobStatus(String jobId, Integer numProcessed){
            this.jobId = jobId;
            this.numProcessed = numProcessed;
        }
    }
    public static class TcpData implements Serializable {
        public String direction;
        public ByteString tcpData;
        public Long seqNumber;
        public String jobId;
        public TcpData (String direction,  Long seqNumber, ByteString tcpData, String jobId){
            this.direction = direction;
            this.tcpData = tcpData;
            this.seqNumber = seqNumber;
            this.jobId = jobId;
        }
    }

    public static class ByteRequest implements Serializable {
        public Long chunkname;
        public Integer size;
        public ByteRequest(Long chunkname, Integer size){
            this.chunkname = chunkname;
            this.size = size;
        }
    }

    public static class DeleteChunk implements Serializable {
        public Long chunkname;
        public DeleteChunk(Long chunkname){
            this.chunkname = chunkname;
        }
    }

    public static class PacketMsg implements Serializable {
        public Long flowHash;
        public Packet packet;
        public String jobId;
        public PacketMsg(Long flowHash, Packet packet, String jobId) {
            this.flowHash = flowHash;
            this.packet = packet;
            this.jobId= jobId;
        }
    }
}
