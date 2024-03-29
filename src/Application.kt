package ru.gs.mytests

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.time.delay
import java.time.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
        masking = false
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
    }

    routing {
        val onQueue: AtomicInteger = AtomicInteger(0)
        val wsConnections = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketSession>())

        get("/") {
            call.respondText("HELLO WORLD with Netty!", contentType = ContentType.Text.Plain)
        }
        get("/queue"){
            call.respondText("""{
                |  "messagesCount":${onQueue.get()},
                |  "connections":[${wsConnections.map { "\n    \"${it.hashCode()}\"" }.joinToString(", ")}
                |  ]
                |}""".trimMargin(), contentType = ContentType.Application.Json)
        }


        webSocket("/chat") {
            val user = this.hashCode()
            println("$user JOINED")
            wsConnections += this

            async {
                delay(Duration.ofSeconds(2))
                send(Frame.Text("Welcome from server after 2 sec"))
//                close(CloseReason(CloseReason.Codes.UNEXPECTED_CONDITION, "test close"))
            }
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            onQueue.incrementAndGet()
                            val text = frame.readText()
                            val msg = "$user say: $text"
                            println()
                            println(msg)
                            // Iterate over all the connections
                            println("broadcast to ${wsConnections.count()} online users")
                            for (conn in wsConnections) {
                                print("send to ${conn.hashCode()}")
                                conn.outgoing.send(Frame.Text(msg))
                                println(" - succeed!")
                            }
                            onQueue.decrementAndGet()
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("connection closed [${closeReason.await()}] for $user")
            } catch (e: Throwable) {
                println("connection interrupted [${closeReason.await()}] for $user")
                e.printStackTrace()
            } finally {
                println("$user LEAVE")
                wsConnections -= this
            }
        }
    }
}

