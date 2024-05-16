package top.gorojack.brokerdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import top.gorojack.brokerdemo.ui.theme.BrokerDemoTheme

data class MyMessage(
    val topic: String?,
    val message: MqttMessage?
)

class MainActivity : ComponentActivity() {
    private lateinit var mqttClient: MqttAndroidClient
    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this@MainActivity, BrokerServer::class.java))
        enableEdgeToEdge()
        setContent {
            var url by remember { mutableStateOf("") }
            var clientId by remember { mutableStateOf("") }
            var topic by remember { mutableStateOf("") }
            var message by remember { mutableStateOf("") }
            val messageList = remember { mutableStateListOf<MyMessage>() }
            val lazyListState = rememberLazyListState()
            LaunchedEffect(messageList.size) {
                lazyListState.animateScrollToItem(messageList.size)
            }
            BrokerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(10.dp)
                    ) {
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(text = "Url") })
                        TextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text(text = "ClientId") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    mqttClient = MqttAndroidClient(this@MainActivity, url, clientId)
                                    mqttClient.setCallback(object : MqttCallback {
                                        override fun connectionLost(cause: Throwable?) {
                                            Log.d(TAG, "阿偶,连接丢失了")
                                        }

                                        override fun messageArrived(
                                            topic: String?,
                                            message: MqttMessage?
                                        ) {
                                            messageList.add(MyMessage(topic, message))
                                            Log.d(
                                                TAG,
                                                "收到消息啦: $topic ${
                                                    message?.payload?.toString(Charsets.UTF_8)
                                                }"
                                            )
                                        }

                                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                                            Log.d(TAG, "发送成功")
                                        }
                                    })
                                    val conOpt = MqttConnectOptions()
                                    conOpt.isCleanSession = true
                                    conOpt.connectionTimeout = 10
                                    conOpt.keepAliveInterval = 20
                                    mqttClient.connect(conOpt, null, object : IMqttActionListener {
                                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                                            Log.d(TAG, "连接成功")
                                        }

                                        override fun onFailure(
                                            asyncActionToken: IMqttToken?,
                                            exception: Throwable?
                                        ) {
                                            Log.d(TAG, "因为${exception?.message} 连接失败了")
                                        }
                                    })
                                }) {
                                    Icon(Icons.Rounded.Add, null)
                                }
                            }
                        )
                        TextField(value = topic,
                            onValueChange = { topic = it },
                            label = { Text(text = "Topic") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    mqttClient.subscribe(topic, 0)
                                }) {
                                    Icon(Icons.Rounded.Notifications, null)
                                }
                            })
                        TextField(value = message,
                            onValueChange = { message = it },
                            label = { Text(text = "Message") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val msg = MqttMessage(message.toByteArray(Charsets.UTF_8))
                                    mqttClient.publish(topic, msg)
                                }) {
                                    Icon(Icons.Rounded.Send, null)
                                }
                            })
                        LazyColumn(state = lazyListState) {
                            item {
                                messageList.forEach { msg ->
                                    Card(modifier = Modifier.padding(10.dp)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "${msg.topic}")
                                            Text(
                                                text = msg.message?.payload?.toString(Charsets.UTF_8)
                                                    ?: ""
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
