package nl.wbaa.auth.token

import java.io.{File, PrintWriter}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.{Failure, Success, Try}

trait TokenPersist[A] {
  /**
    * store a token
    *
    * @param a - token to store
    */
  def store(a: A)

  /**
    * remove a token
    *
    * @param a - token to remove
    */
  def remove(a: A)

  /**
    * read all token
    *
    * @return seq of stored tokens
    */
  def readAll: Seq[A]
}


object TokenPersistProvider {


  import spray.json._
  import DefaultJsonProtocol._

  implicit val jsonToken2: RootJsonFormat[Token] = jsonFormat3(Token)
  implicit val jsonToken: RootJsonFormat[User] = jsonFormat2(User)

  /**
    * Token persist impl storing tokens in a file
    * @param fileName - the file name to store token
    * @return TokenPersist impl.
    */
  def listBufferTokenStorage(fileName: String = "tokens.json"): TokenPersist[User] = new TokenPersist[User] {

    private val storage = readFromFile()

    override def store(user: User): Unit = {
      storage += user
      storeOnFile()
    }

    override def readAll: Seq[User] = storage

    override def remove(user: User): Unit = {
      storage -= user
      storeOnFile()
    }


    private def storeOnFile(): Unit = {
      val writer = new PrintWriter(new File(fileName))
      storage.foreach(o => writer.write(o.toJson.toString + "\n"))
      writer.close()
    }

    private def readFromFile(): ListBuffer[User] = {
      Try {
        Source.fromFile(fileName).mkString match {
          case json: String if json != null =>
            json.split("\n").map(_.parseJson.convertTo[User]).to[ListBuffer]
          case _ => ListBuffer.empty[User]
        }
      } match {
        case Success(v) => v
        case Failure(ex) => ListBuffer.empty[User]
      }
    }
  }
}
