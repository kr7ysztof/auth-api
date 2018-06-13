package nl.wbaa.auth.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import nl.wbaa.auth.actor.CredentialProviderActor.{CheckExpiredToken, GenerateToken, RemoveToken}
import nl.wbaa.auth.token.{Token, TokenPersist, TokenProvider, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.FiniteDuration

class RgwCredentialProviderActorTest
  extends TestKit(ActorSystem("RgwSystem"))
    with ImplicitSender
    with ScalaFutures
    with WordSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val testUser = User("testUser", Token("123", "abc", 720000))


  def credentialProviderTestActorPropsWithoutScheduler(tokenProvider: TokenProvider, storage: TokenPersist[User]): Props = {
    Props(new CredentialProviderActor(tokenProvider, storage) {
      override def preStart(): Unit = {}
      override def scheduleNextCheck(delay: FiniteDuration): Unit = {}
    })
  }

  "Credential provider actor" must {
    "generate credential" in {
      val tokenProvider = stub[TokenProvider]
      val storage = mock[TokenPersist[User]]
      tokenProvider.generateToken _ when(testUser.userId, testUser.credential.expirationTime.toInt) returns testUser
      storage.store _ expects testUser
      val credentialProviderActor = system.actorOf(credentialProviderTestActorPropsWithoutScheduler(tokenProvider, storage))
      credentialProviderActor ! GenerateToken(testUser.userId, testUser.credential.expirationTime.toInt)
      expectMsg(testUser)
    }

    "remove credential" in {
      val credentialProvider = mock[TokenProvider]
      val storage = mock[TokenPersist[User]]
      credentialProvider.removeToken _ expects(testUser.userId, testUser.credential.accessKey)
      val credentialProviderActor = system.actorOf(credentialProviderTestActorPropsWithoutScheduler(credentialProvider, storage))
      credentialProviderActor ! RemoveToken(testUser.userId, testUser.credential.accessKey)
      expectNoMessage()
    }

    "check expired credential" in {
      val credentialProvider = mock[TokenProvider]
      val storage = stub[TokenPersist[User]]
      credentialProvider.removeToken _ expects(testUser.userId, testUser.credential.accessKey)
      storage.readAll _ when() returns List(testUser)
      val credentialProviderActor = system.actorOf(credentialProviderTestActorPropsWithoutScheduler(credentialProvider, storage))
      credentialProviderActor ! CheckExpiredToken
      expectNoMessage()
    }
  }
}
