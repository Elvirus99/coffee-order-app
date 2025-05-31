package io.ktor.samples.structuredlogging

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import java.util.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

@Serializable
data class Narudzba(
    val id: Int? = null,
    val ime: String,
    val broj: String,
    val kolicina: Int
)


@Serializable
data class Odgovor(val message: String, val ime: String, val broj: String, val kolicina: Int)

object Narudzbe : Table() {
    val id = integer("id").autoIncrement()
    val ime = varchar("ime", 100)
    val broj = varchar("broj", 20)
    val kolicina = integer("kolicina")
    override val primaryKey = PrimaryKey(id)
}

fun Application.module() {
	Database.connect(
    url = System.getenv("DB_URL"),
    driver = "org.postgresql.Driver",
    user = System.getenv("DB_USER"),
    password = System.getenv("DB_PASSWORD")
	)


    transaction {
        createMissingTablesAndColumns(Narudzbe)
    }

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val requestId = UUID.randomUUID()
        call.logger.attach("req.Id", requestId.toString()) {
            call.logger.info("Interceptor[start]")
            proceed()
            call.logger.info("Interceptor[end]")
        }
    }

    routing {
        post("/order") {
            val narudzba = call.receive<Narudzba>()
            call.logger.info("Primljena narudžba: $narudzba")

            transaction {
                Narudzbe.insert {
                    it[ime] = narudzba.ime
                    it[broj] = narudzba.broj
                    it[kolicina] = narudzba.kolicina
                }
            }

            val odgovor = Odgovor("Narudžba primljena", narudzba.ime, narudzba.broj, narudzba.kolicina)
            call.respond(odgovor)
        }

        get("/orders") {
            val sveNarudzbe = transaction {
                Narudzbe.selectAll().map {
                    Narudzba(
                        id = it[Narudzbe.id],
                        ime = it[Narudzbe.ime],
                        broj = it[Narudzbe.broj],
                        kolicina = it[Narudzbe.kolicina]
                    )
                }
            }
            call.respond(sveNarudzbe)
        }

        delete("/order/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Neispravan ID")
                return@delete
            }

            val deleted = transaction {
                Narudzbe.deleteWhere { Narudzbe.id eq id }
            }

            if (deleted > 0) {
                call.respond(HttpStatusCode.OK, "Narudžba obrisana")
            } else {
                call.respond(HttpStatusCode.NotFound, "Narudžba nije pronađena")
            }
        }

        get("/") {
            call.logger.info("Respond[start]")
            call.respondText("HELLO WORLD")
            call.logger.info("Respond[end]")
        }
    }
}


