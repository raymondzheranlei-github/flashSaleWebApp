package com.raymondlzr.flashsale.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

@Slf4j
@Service
public class RedisService {
    @Autowired
    private JedisPool jedisPool;

    /**
     * addLimitMember
     *
     * @param activityId
     * @param userId
     */
    public void addLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.sadd("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
    }

    /**
     * check whether isInLimitMember
     *
     * @param activityId
     * @param userId
     * @return
     */
    public boolean isInLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        boolean sismember = jedisClient.sismember("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
        log.info("userId:{}  activityId:{}  is in Limited List:{}", userId, activityId, sismember);
        return sismember;
    }

    /**
     * removeLimitMember
     *
     * @param activityId
     * @param userId
     */
    public void removeLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.srem("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
    }

    /**
     * Overdue Payment Redis revertStock
     *
     * @param key
     */
    public void revertStock(String key) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.incr(key);
        jedisClient.close();
    }

    /**
     * setValue
     *
     * @param key
     * @param value
     */
    public void setValue(String key, Long value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value.toString());
        jedisClient.close();
    }

    /**
     * setValue
     *
     * @param key
     * @param value
     */
    public void setValue(String key, String value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value);
        jedisClient.close();
    }

    /**
     * getValue
     *
     * @param key
     * @return
     */
    public String getValue(String key) {
        Jedis jedisClient = jedisPool.getResource();
        String value = jedisClient.get(key);
        jedisClient.close();
        return value;
    }

    /**
     * check and deduct item numbers from redis
     *
     * @param key
     * @return
     * @throws Exception
     */
    public boolean stockDeductValidator(String key) {
        Jedis jedisClient = null;
        try {
            jedisClient = jedisPool.getResource();

            String script = "if redis.call('exists',KEYS[1]) == 1 then\n" +
                    "                 local stock = tonumber(redis.call('get', KEYS[1]))\n" +
                    "                 if( stock <=0 ) then\n" +
                    "                    return -1\n" +
                    "                 end;\n" +
                    "                 redis.call('decr',KEYS[1]);\n" +
                    "                 return stock - 1;\n" +
                    "             end;\n" +
                    "             return -1;";


            Long stock = (Long) jedisClient.eval(script, Collections.singletonList(key), Collections.emptyList());
            if (stock < 0) {
                System.out.println("Item is not available");
                return false;
            } else {
                System.out.println("Congratulations! You purchased successfully");
            }
            return true;
        } catch (Throwable throwable) {
            System.out.println("Failed to deduct items：" + throwable.toString());
            return false;
        } finally {
            if (jedisClient != null) {
                jedisClient.close();
            }
        }
    }

    /**
     * get distributed lock
     *
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis jedisClient = jedisPool.getResource();
        String result = jedisClient.set(lockKey, requestId, "NX", "PX", expireTime);
        jedisClient.close();
        if ("OK".equals(result)) {
            return true;
        }
        return false;
    }

    /**
     * release distributed lock
     * iff both key and value are same as the key and value of current lock
     * @param lockKey   lock
     * @param requestId request Id
     * @return release or not
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedisClient = jedisPool.getResource();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = (Long) jedisClient.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        jedisClient.close();
        if (result == 1L) {
            return true;
        }
        return false;
    }
}
