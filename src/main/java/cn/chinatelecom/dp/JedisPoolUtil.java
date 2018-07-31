package cn.chinatelecom.dp;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class JedisPoolUtil {


    public static JedisSentinelPool pool = null;

    public static Properties getJedisProperties() {

        Properties config = new Properties();
        InputStream is = null;
        try {
            is = JedisPoolUtil.class.getClassLoader().getResourceAsStream("cacheConfig.properties");
            config.load(is);
        } catch (IOException e) {
            //logger.error("", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                   // logger.error("", e);
                }
            }
        }
        return config;
    }

    /**
     * 创建连接池
     */
    public static void createJedisPool() {
        // 建立连接池配置参数
        JedisPoolConfig config = new JedisPoolConfig();
       // Properties prop = getJedisProperties();
        // 设置最大连接数
        config.setMaxTotal(150);
        // 设置最大阻塞时间，记住是毫秒数milliseconds
        config.setMaxWaitMillis(100000);
        // 设置空间连接
        config.setMaxIdle(100);

        config.setMinIdle(10);
        // jedis实例是否可用
        //boolean borrow = prop.getProperty("TEST_ON_BORROW") == "false" ? false : true;
        config.setTestOnBorrow(true);
        // 创建连接池
//      pool = new JedisPool(config, prop.getProperty("ADDR"), StringUtil.nullToInteger(prop.getProperty("PORT")), StringUtil.nullToInteger(prop.getProperty("TIMEOUT")));// 线程数量限制，IP地址，端口，超时时间
        //获取redis密码


        String masterName = "todp-master";
        Set<String> sentinels = new HashSet<String>();
        sentinels.add("10.142.78.104:26389");
        sentinels.add("10.142.78.103:26389");
        sentinels.add("10.142.78.102:26389");
        pool = new JedisSentinelPool(masterName, sentinels, config);
    }

    /**
     * 在多线程环境同步初始化
     */
    private static synchronized void poolInit() {
        if (pool == null)
            createJedisPool();
    }

    /**
     * 获取一个jedis 对象
     *
     * @return
     */
    public static Jedis getJedis() {
        if (pool == null)
            poolInit();
        return pool.getResource();
    }



    public static void main(String[] args) {
        Jedis jedis = getJedis();

    }


}
