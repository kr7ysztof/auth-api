package nl.wbaa.auth

import akka.http.scaladsl.server.{Route, RouteConcatenation}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.typesafe.config.ConfigFactory
import io.swagger.models.Scheme
import nl.wbaa.auth.api.CephApi

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait Routes extends RouteConcatenation {
  this: Actors with CoreActorSystem =>

  val routes: Route = cors() {
    implicit val exContext: ExecutionContextExecutor = system.dispatcher
    implicit val timeout: Timeout = Timeout(5.seconds)
    SwaggerDoc.routes ~
      new CephApi(credentialProviderActor).routes
  }
}

object SwaggerDoc extends SwaggerHttpService {
  private val config = ConfigFactory.load().getConfig("api.server")
  override val apiClasses: Set[Class[_]] = Set(classOf[CephApi])
  override val host = s"${config.getString("hostname")}:${config.getString("port")}" //the url of your api, not swagger's json endpoint
  override val basePath = "/" //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info() //provides license and other description details
  override val schemes = List(Scheme.HTTPS)
}
