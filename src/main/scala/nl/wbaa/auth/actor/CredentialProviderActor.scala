package nl.wbaa.auth.actor

import akka.actor.{Actor, ActorLogging, Props}
import nl.wbaa.auth.actor.CredentialProviderActor.{CheckExpiredToken, GenerateToken, RemoveToken}
import nl.wbaa.auth.token.{TokenPersist, TokenProvider, User}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object CredentialProviderActor {

  case class GenerateToken(userId: String, expirationTimeInMs: Int)

  case class RemoveToken(userId: String, accessKey: String)

  case class CheckExpiredToken()

  def props(tokenProvider: TokenProvider, storage: TokenPersist[User]): Props =
    Props(new CredentialProviderActor(tokenProvider, storage))
}

/**
  * Credential provider actor
  *
  * @param tokenProvider - any implemetation of the TokenProvider
  */
class CredentialProviderActor(tokenProvider: TokenProvider, storage: TokenPersist[User])
  extends Actor with ActorLogging {

  private val scheduleDelay: FiniteDuration = 1.minute

  private implicit val exCnx: ExecutionContextExecutor = context.system.dispatcher

  override def preStart(): Unit = {
    self ! CheckExpiredToken
    super.preStart()
  }

  override def receive: Receive = {
    case GenerateToken(userId, expirationTimeInMs) =>
      val user = tokenProvider.generateToken(userId, expirationTimeInMs)
      storage.store(user)
      log.info("creating accessKey {} for user {}", user.userId, user.credential.accessKey)
      sender ! user
    case RemoveToken(userId, accessKey) =>
      tokenProvider.removeToken(userId, accessKey)
      log.info("accessKey={} for user={} removed", accessKey, userId)
    case CheckExpiredToken =>
      log.debug("checking expiration tokens ...")
      val currentTime = System.currentTimeMillis()
      val tokens = storage.readAll
      log.debug("there are {} tokens {}", tokens.size, tokens)
      tokens.filter(_.credential.expirationTime <= currentTime).foreach(user => {
        storage.remove(user)
        self ! RemoveToken(user.userId, user.credential.accessKey)
        log.info("removing accessKey {} for user {}", user.credential.accessKey, user.userId)
      })
      scheduleNextCheck()
  }

  def scheduleNextCheck(delay: FiniteDuration = scheduleDelay): Unit = {
    context.system.scheduler.scheduleOnce(delay, self, CheckExpiredToken)  }
}


