package nl.wbaa.auth.token

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.twonote.rgwadmin4j.{RgwAdmin, RgwAdminBuilder}

import scala.collection.JavaConverters._

object RgwTokenProvider {

  def apply(adminId: String,
            adminAccessKey: String,
            adminSecretKey: String,
            endpoint: String): TokenProvider =
    new RgwTokenProvider(adminId, adminAccessKey, adminSecretKey, endpoint)

  def apply(config: Config): TokenProvider = {
    val rqwConfig = config.getConfig("rgw.admin")
    new RgwTokenProvider(rqwConfig.getString("id"),
      rqwConfig.getString("accessKey"),
      rqwConfig.getString("secretKey"),
      rqwConfig.getString("endpoint"))
  }
}

/**
  * A util to manage radosgw user keys.
  * To be able to manage keys a radosgw admin user is needed (which has caps - user=*, bucket=*
  *
  * @param adminId        - admin user able to generate keys
  * @param adminAccessKey - admin access key
  * @param adminSecretKey - admin secret key
  * @param endpoint       - radosgw endpoint
  */
class RgwTokenProvider(adminId: String,
                       adminAccessKey: String,
                       adminSecretKey: String,
                       endpoint: String)
  extends TokenProvider with LazyLogging {

  protected val RGW_ADMIN: RgwAdmin =
    new RgwAdminBuilder()
      .accessKey(adminAccessKey)
      .secretKey(adminSecretKey)
      .endpoint(endpoint)
      .build

  def generateToken(userId: String, expirationTimeInMs: Long): User = {
    val accessKey = UUID.randomUUID().toString
    val secretKey = UUID.randomUUID().toString
    val credential = RGW_ADMIN.createS3Credential(adminId, accessKey, secretKey).asScala.filter(_.getAccessKey.equals(accessKey)).head
    val expirationTime = expirationTimeInMs + System.currentTimeMillis()
    val user = User(userId, Token(credential.getAccessKey, credential.getSecretKey, expirationTime))
    logger.debug("generate accessKey {} for user {}", user.credential.accessKey, user.userId)
    user
  }

  def removeToken(userId: String, accessToken: String): Unit = {
    RGW_ADMIN.removeS3Credential(adminId, accessToken)
    logger.debug("removed accessKey {} for user {}", accessToken, userId)
  }
}
