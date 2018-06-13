package nl.wbaa.auth.token


trait TokenProvider {
  /**
    * Generate a credential keys
    *
    * @param userId             - the user id to generate credential
    * @param expirationTimeInMs - how long the credential is valid
    * @return the user with the generated token
    */
  def generateToken(userId: String, expirationTimeInMs: Long): User

  /**
    * Remove the access key for the user id
    *
    * @param userId    - the user id
    * @param accessToken - the access token
    */
  def removeToken(userId: String, accessToken: String): Unit
}


case class User(userId: String, credential: Token)

case class Token(accessKey: String, secretKey: String, expirationTime: Long)




