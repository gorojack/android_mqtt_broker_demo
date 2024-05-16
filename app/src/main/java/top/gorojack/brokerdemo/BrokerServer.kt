package top.gorojack.brokerdemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.moquette.BrokerConstants
import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import top.gorojack.brokerdemo.ServerInstance.service
import java.io.File
import java.io.IOException
import java.net.BindException
import java.util.Properties
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

object ServerInstance {
    const val MQTT_SERVER_PORT = 1883
    val service by lazy { Server() }
}

class BrokerServer : Service() {

    private val TAG = BrokerServer::class.java.simpleName
    private var thread: Thread? = null
    private var mqttBroker: MQTTBroker? = null

    private val properties by lazy {
        Properties().apply {
            set(IConfig.PORT_PROPERTY_NAME, "${ServerInstance.MQTT_SERVER_PORT}")
            set(BrokerConstants.NETTY_EPOLL_PROPERTY_NAME, true)
            set(BrokerConstants.BUFFER_FLUSH_MS_PROPERTY_NAME, true)
            set(BrokerConstants.IMMEDIATE_BUFFER_FLUSH_PROPERTY_NAME, true.toString())
            set(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME, 8092)
            set(BrokerConstants.INFLIGHT_WINDOW_SIZE, 20)
            set(
                IConfig.DATA_PATH_PROPERTY_NAME, "${
                    getDir("mqtt", 0)
                        .absolutePath
                }${File.separator}${BrokerConstants.DEFAULT_MOQUETTE_STORE_H2_DB_FILENAME}"
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return startBroker()
    }

    override fun onDestroy() {
        if (thread != null) {
            mqttBroker?.stopServer()
            thread?.interrupt()
            Log.d(TAG, "停止Broker")
        }
        super.onDestroy()
    }

    private fun startBroker(): Int {
        try {
            mqttBroker = MQTTBroker(properties)
            val futureTask = FutureTask(mqttBroker)
            if (thread == null || thread?.isAlive == true) {
                thread = Thread(futureTask)
                thread?.name = "MQTT_Server"
                thread?.start()
                if (futureTask.get()) {
                    Log.d(TAG, "Broker启动成功")
                } else {
                    Log.d(TAG, "Broker启动失败")
                }
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
}

class MQTTBroker(private val config: Properties) : Callable<Boolean> {

    fun stopServer() {
        try {
            service.stopServer()
        } catch (e: Exception) {
            e.message
        }
    }

    override fun call(): Boolean {
        try {
            service.startServer(config)
            return true
        } catch (e: BindException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}