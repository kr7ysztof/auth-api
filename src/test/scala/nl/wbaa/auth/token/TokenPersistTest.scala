package nl.wbaa.auth.token

import java.io.File
import java.nio.file.{Files, Paths}

import org.scalatest.{Matchers, WordSpecLike}

class TokenPersistTest
  extends WordSpecLike
    with Matchers {


  val testUser = User("testUser", Token("123", "abc", 720000))
  val testUser2 = User("testUser2", Token("12345", "abcefg", 520000))


  "Token persistent " must {
    "store and remove users" in {
      val fileName = "testTokens.json"
      Files.deleteIfExists(Paths.get(fileName))
      val storage = TokenPersistProvider.listBufferTokenStorage(fileName)
      storage.store(testUser)
      storage.readAll should contain theSameElementsAs List(testUser)
      storage.store(testUser2)
      storage.readAll should contain theSameElementsAs List(testUser2, testUser)
      storage.remove(testUser)
      storage.readAll should contain theSameElementsAs List(testUser2)
      storage.remove(testUser2)
      storage.readAll.size shouldBe 0
      Files.deleteIfExists(Paths.get(fileName))
    }
  }
}
