package nl.wbaa.auth.api

import java.nio.charset.StandardCharsets.UTF_8

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import akka.util.Timeout
import com.tresata.akka.http.spnego.Tokens
import nl.wbaa.auth.actor.CredentialProviderActor.GenerateToken
import nl.wbaa.auth.token.{Token, User}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class CephApiTest extends WordSpec with Matchers with ScalatestRouteTest {

  private implicit val timeout: Timeout = Timeout(5.seconds)
  private val tokens = new Tokens(3600 * 1000, "secret".getBytes(UTF_8))
  private val token = tokens.create("test@EXAMPLE.COM")
  private val cookieName = "akka.http.spnego"
  private val cookieValue = tokens.serialize(token)
  private val testUser = User("testUser", Token("12345", "abcd", 7200000))
  private val credentialProviderActor = TestProbe("credentialProvider")
  credentialProviderActor.setAutoPilot((sender: ActorRef, msg: Any) => msg match {
    case GenerateToken(_, expirationTime) => {
      sender ! (expirationTime match {
        case 100 => User(testUser.userId, Token(testUser.credential.accessKey, testUser.credential.secretKey, 100))
        case _ => testUser
      })
    }
      TestActor.KeepRunning
  })


  private val cephRouts = new CephApi(credentialProviderActor.ref).routes

  "Ceph api" should {
    "request for a credential is rejected because lack of authentication" in {
      Get("/ceph/credential/testBucket") ~> cephRouts ~> check {
        rejection shouldBe a[AuthenticationFailedRejection]
      }
    }

    "request for a credential is ok" in {
      Get("/ceph/credential/testBucket") ~> Cookie(cookieName -> cookieValue) ~> cephRouts ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          s"""{"expirationTime":7200000,""" +
            s""""accessKey":"${testUser.credential.accessKey}",""" +
            s""""secretKey":"${testUser.credential.secretKey}",""" +
            s""""bucketName":"testBucket",""" +
            s""""user":"${token.principal}"}"""
      }
    }

    "request for a credential with set the expirationTimeInMs" in {
      Get("/ceph/credential/testBucket?expirationTimeInMs=100") ~> Cookie(cookieName -> cookieValue) ~> cephRouts ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual
          s"""{"expirationTime":100,""" +
            s""""accessKey":"${testUser.credential.accessKey}",""" +
            s""""secretKey":"${testUser.credential.secretKey}",""" +
            s""""bucketName":"testBucket",""" +
            s""""user":"${token.principal}"}"""
      }
    }
  }
}

