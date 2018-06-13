package nl.wbaa.auth

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import nl.wbaa.auth.actor.CredentialProviderActor
import nl.wbaa.auth.token._

trait CoreActorSystem {
  implicit def system: ActorSystem = ActorSystem()

  sys.addShutdownHook {
    system.terminate()
  }
  val config: Config = ConfigFactory.load()
}

trait Actors {
  this: CoreActorSystem =>
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val tokenProvider = RgwTokenProvider(config)
  private val storage = TokenPersistProvider.listBufferTokenStorage(config.getString("storage.token.path"))

  val credentialProviderActor: ActorRef = system.actorOf(CredentialProviderActor.props(tokenProvider, storage))
}
