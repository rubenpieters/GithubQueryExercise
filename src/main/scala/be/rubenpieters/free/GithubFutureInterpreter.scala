package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.Xor
import cats.~>
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser._
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by ruben on 23/10/2016.
  */
class GithubFutureInterpreter(implicit ec: ExecutionContext) extends (GithubApiDsl ~> Future) {
  // some hardcoding for now for simplicity

  val client = NingWSClient()

  override def apply[A](fa: GithubApiDsl[A]): Future[A] = fa match {
    case ListIssues(owner, repo) =>
      LoggerFactory.getLogger(getClass).debug(s"get issues owner: $owner repo: $repo")
      client.getAndDecode[List[Issue]](s"https://api.github.com/repos/$owner/$repo/issues")
    case GetComments(owner, repo, issueNr) =>
      LoggerFactory.getLogger(getClass).debug(s"get comments owner: $owner repo: $repo issueNr: $issueNr")
      client.getAndDecode[List[Comment]](s"https://api.github.com/repos/$owner/$repo/issues/$issueNr/comments")
    case GetUser(userRef: UserReference) =>
      LoggerFactory.getLogger(getClass).debug(s"get user userRef: $userRef")
      client.getAndDecode[User](s"https://api.github.com/user/$userRef")
  }

  implicit class EnrichedNingWsClient(client: NingWSClient) {
    def getAndDecode[A](url: String)(implicit ec: ExecutionContext, decoder: Decoder[A]): Future[GithubApiDslResult[A]] =
      client
        .url(url)
        .get()
        .map( wSResponse =>
          for {
            handledResp <- handleWsResponse(wSResponse)
            decodedResp <- decode(handledResp)
          } yield decodedResp
        )
  }

  def handleWsResponse(wSResponse: WSResponse): Xor[Throwable, String] = {
    if (! (200 to 299).contains(wSResponse.status)) {
      Xor.Left(new RuntimeException(s"Received unexpected status ${wSResponse.status} : ${wSResponse.body}"))
    } else {
      Xor.Right(wSResponse.body)
    }
  }
}