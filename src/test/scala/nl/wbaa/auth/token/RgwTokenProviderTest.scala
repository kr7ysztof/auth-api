package nl.wbaa.auth.token


import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import org.twonote.rgwadmin4j.RgwAdmin
import org.twonote.rgwadmin4j.model.S3Credential

class RgwTokenProviderTest
  extends WordSpecLike
    with Matchers
    with MockFactory {

  val userId = "userId"

  class MockRgwTokenProvider
    extends RgwTokenProvider(userId, "adminAccessKey", "adminSecretKey", "http://endpoint:123") {

    class S3CredentialTest(accessKey: String, secretKey: String) extends S3Credential {
      override def getAccessKey: String = accessKey

      override def getSecretKey: String = secretKey

      override def getUserId: String = userId
    }

  }

  "RgwCredentialProvider" must {
    "generate credential" in {

      val mockRgwCredentialProvider = new MockRgwTokenProvider {
        override protected val RGW_ADMIN: RgwAdmin = mock[RgwAdmin]
        (RGW_ADMIN.createS3Credential(_: String, _: String, _: String))
          .expects(*, *, *) onCall {
          (userId: String, ak: String, sk: String) => java.util.Arrays.asList(new S3CredentialTest(ak, sk).asInstanceOf[S3Credential])
        }
      }
      val expirationTime = 100000
      val user = mockRgwCredentialProvider.generateToken(userId, expirationTime)
      user.userId shouldEqual userId
      val currentTime = System.currentTimeMillis()
      (user.credential.expirationTime > currentTime) shouldBe true
      (user.credential.expirationTime < currentTime + expirationTime) shouldBe true
    }

    "remove credential" in {
      val accessKeyToRemove = "1234"
      val mockRgwTokenProvider = new MockRgwTokenProvider {
        override protected val RGW_ADMIN: RgwAdmin = mock[RgwAdmin]
        (RGW_ADMIN.removeS3Credential(_: String, _: String)).expects(userId, accessKeyToRemove)
      }
      mockRgwTokenProvider.removeToken(userId, accessKeyToRemove)
    }
  }
}
