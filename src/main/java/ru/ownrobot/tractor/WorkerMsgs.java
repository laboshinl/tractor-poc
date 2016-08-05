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
    public static class TcpData implements Serializable {
        public String direction;
        public ByteString tcpData;
        public Long seqNumber;
        public TcpData (String direction,  Long seqNumber, ByteString tcpData){
            this.direction = direction;
            this.tcpData = tcpData;
            this.seqNumber = seqNumber;
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
}
