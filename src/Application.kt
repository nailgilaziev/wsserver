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

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf

fun Application.module() {
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        val wsConnections = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketSession>())
        webSocket("/chat") {
            println("user joined $this")
            wsConnections += this
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            // Iterate over all the connections
                            for (conn in wsConnections) {
                                conn.outgoing.send(Frame.Text(text))
                            }
                        }
                    }
                }
            } finally {
                println("user leave $this")
                wsConnections -= this
            }
        }
    }
}

