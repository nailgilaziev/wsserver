package ru.gs.mytests

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        masking = true
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD with Jetty!", contentType = ContentType.Text.Plain)
        }

        val wsConnections = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketSession>())
        webSocket("/chat") {
            val user = this.hashCode()
            println("$user JOINED")
            wsConnections += this
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val msg = "$user say: $text"
                            println(msg)
                            // Iterate over all the connections
                            for (conn in wsConnections) {
                                conn.outgoing.send(Frame.Text(msg))
                            }
                        }
                    }
                }
            } catch (ex: Exception){
                println("EXCEPTION: $ex")
            } finally {
                println("$user LEAVE")
                wsConnections -= this
            }
        }
    }
}

