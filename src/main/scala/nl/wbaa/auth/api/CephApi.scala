package nl.wbaa.auth.api

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.tresata.akka.http.spnego.SpnegoDirectives.spnegoAuthenticate
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path
import nl.wbaa.auth.actor.CredentialProviderActor.GenerateToken
import nl.wbaa.auth.token.User
import spray.json._

import scala.concurrent.ExecutionContext

object CephApi {

  case class Credential(user: String, bucketName: String, accessKey: String, secretKey: String, expirationTime: Long)

  import spray.json.DefaultJsonProtocol._

  implicit val jsonToken: RootJsonFormat[Credential] = jsonFormat5(Credential)
}

@Api(value = "/ceph",
  produces = "application/json")
@Path("/ceph")
class CephApi(crdentialProviderActor: ActorRef)(implicit executionContext: ExecutionContext, timeout: Timeout) extends LazyLogging {

  import CephApi._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  var routes: Route = getCredentialKeys

  @Path("credential/{bucketName}")
  @ApiOperation(value = "Get a token for a ceph bucket",
    notes = "notes",
    nickname = "token",
    httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "bucketName",
        value = "The backet name to get a token",
        required = true,
        dataType = "string",
        paramType = "path"),
      new ApiImplicitParam(
        name = "expirationTimeInMs",
        value = "how the credential has to be valid (default=2h)",
        required = false,
        dataType = "integer",
        paramType = "query")))
  @ApiResponses(
    Array(
      new ApiResponse(code = 200,
        message = "Return the token for a bucket",
        response = classOf[Credential]),
      new ApiResponse(code = 401, message = "Invalid credentials"),
      new ApiResponse(code = 404, message = "Bucket not found"),
      new ApiResponse(code = 500, message = "Internal server error")))
  def getCredentialKeys: Route = logRequestResult("debug") {
    spnegoAuthenticate() { token =>
      get {
        path("ceph" / "credential" / Segment) { bucketName =>
          parameter('expirationTimeInMs ? 7200000) { credentialExpirationTimeInMs =>
            val userCredential = crdentialProviderActor ? GenerateToken(token.principal, credentialExpirationTimeInMs)
            onSuccess(userCredential) {
              case user: User =>
                complete(Credential(token.principal, bucketName, user.credential.accessKey, user.credential.secretKey, user.credential.expirationTime))
              case message =>
                logger.error("getCredentialKeys error = {}", message)
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
    }
  }
}
