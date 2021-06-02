package com.raymondlzr.flashsale.util;

/**
 * Twitter distributed ID series snowflake algorithm
 **/
public class SnowFlake {

    /**
     * Start of the time stamp
     */
    private final static long START_STMP = 1480166465631L;

    /**
     * each section versus needed number of bits
     */
    private final static long SEQUENCE_BIT = 12; //SEQUENCE_BIT need 12 bits
    private final static long MACHINE_BIT = 5;   //number of bits need 5 bits
    private final static long DATACENTER_BIT = 5;// datacenter need 5 bits

    /**
     * Max value in each section
     */
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * left shift in each section
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long datacenterId;
    private long machineId;
    private long sequence = 0L;
    private long lastStmp = -1L;

    public SnowFlake(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * generate next ID
     *
     * @return
     */
    public synchronized long nextId() {
        long currStmp = getNewstmp();
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currStmp == lastStmp) {
            //In same ms, the sequence number will increment
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //In same milliseconds, sequence number has reached the maximum number
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            //In different milliseconds, sequence number is set as 0
            sequence = 0L;
        }

        lastStmp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT //Timestamp section
                | datacenterId << DATACENTER_LEFT       //datacenter
                | machineId << MACHINE_LEFT
                | sequence;
    }

    private long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    private long getNewstmp() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(2, 1);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            System.out.println(snowFlake.nextId());
        }

        System.out.println("Total used time：" + (System.currentTimeMillis() - start));
    }
}