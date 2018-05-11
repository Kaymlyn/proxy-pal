package com.knightowlgames.proxypal;

import com.squareup.okhttp.OkHttpClient;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;


public final class HttpManager
{

    private final static Logger LOG = LoggerFactory.getLogger(HttpManager.class);
    private final static Object myLock = new Object();
    private static volatile OkHttpClient client;

    private HttpManager()
    {
    }

    public static OkHttpClient getHttpClient()
    {
        if (client == null)
        {
            synchronized (HttpManager.myLock)
            {
                if (client == null)
                {

                    client = new OkHttpClient();

                    client.setConnectTimeout(500, TimeUnit.MILLISECONDS);
                    client.setReadTimeout(1000, TimeUnit.MILLISECONDS);
                    client.setWriteTimeout(10, TimeUnit.SECONDS);
                }
            }
        }
        return client;
    }

    static public boolean isWorking(boolean stats)
    {
        int queued = client.getDispatcher().getQueuedCallCount();
        int running = client.getDispatcher().getRunningCallCount();

        if (stats)
        {
            LOG.info("Queued: " + queued + " | Running: " + running);
        }

        return (queued > 0 || running > 0);
    }
}