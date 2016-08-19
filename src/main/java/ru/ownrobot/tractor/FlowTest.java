package ru.ownrobot.tractor;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by laboshinl on 8/19/16.
 */
public class FlowTest {
    public static void main(String[] args){
        byte[] a = {11, 12, 13, 14};
        byte[] b = {15, 16, 17, 18};
        int c = 65535;
        int d = 443;
        System.out.println(hash(a,b, c, d));
        System.out.println(hash(b,a,d,c));
    }

//    public static Integer hash(Integer x, Integer y)
//    {
//        Integer d = x-y;
//        Integer min = y + (d & d>>31); // fast branchless min
//        Integer max = x - (d & d>>31); // fast branchless max
//
//        return max<<32 | min;
//    }

    public static Long hash(byte[] ipSrc, byte[] ipDst, Integer portSrc, Integer portDst) {
        byte[] a1 = ByteBuffer.allocate(4).putInt(portSrc).array();
        byte[] b1 = ByteBuffer.allocate(4).putInt(portDst).array();
        byte[] a2 = ByteBuffer.allocate(8).put(ipSrc).put(a1).array();
        byte[] b2 = ByteBuffer.allocate(8).put(ipDst).put(b1).array();
        Long a = ByteBuffer.wrap(a2).getLong();
        Long b = ByteBuffer.wrap(b2).getLong();

        Long d = Math.abs(a-b);
        Long min = a + (d & d>>63); // fast branchless min
        Long max = b - (d & d>>63); // fast branchless max

        return max<<64 | min;
    }
}
