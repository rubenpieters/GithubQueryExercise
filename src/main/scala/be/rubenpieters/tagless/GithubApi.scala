package be.rubenpieters.tagless

import cats.implicits._
import be.rubenpieters.free._
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.{Monad, Traverse}
import cats.data.{Xor, XorT}

/**
  * Created by ruben on 23/10/2016.
  */
object GithubApi {
  type GithubApiResult[A] = Xor[Throwable, A]

  def listIssues(owner: Owner, repo: Repo): Term[GithubApiAlg, GithubApiResult[List[Issue]]] = new Term[GithubApiAlg, GithubApiResult[List[Issue]]] {
    def apply[F[+_]](F: GithubApiAlg[F]): F[GithubApiResult[List[Issue]]] =
      F.listIssues(owner, repo)
  }

  def getComments(owner: Owner, repo: Repo, issueNr: IssueNumber): Term[GithubApiAlg, GithubApiResult[List[Comment]]] = new Term[GithubApiAlg, GithubApiResult[List[Comment]]] {
    def apply[F[+_]](F: GithubApiAlg[F]): F[GithubApiResult[List[Comment]]] =
      F.getComments(owner, repo, issueNr)
  }

  def getUser(userRef: UserReference): Term[GithubApiAlg, GithubApiResult[User]] = new Term[GithubApiAlg, GithubApiResult[User]] {
    def apply[F[+_]](F: GithubApiAlg[F]): F[GithubApiResult[User]] =
      F.getUser(userRef)
  }

  def allUsers[F[_]](owner: Owner, repo: Repo)(implicit monad: Monad[F]): GithubApiAlg[F] => F[GithubApiResult[List[(Issue, List[(Comment, User)])]]] = alg => {
    (for {
      issues <- XorT[F, Throwable, List[Issue]](
        alg.listIssues(owner, repo))

      issueComments <-
      issues.traverse(issue =>
        XorT[F, Throwable, List[Comment]](alg.getComments(owner, repo, issue.number))
          .map((issue, _))
      ): XorT[F, Throwable, List[(Issue, List[Comment])]]

      users <-
      issueComments.traverse { case (issue, comments) =>
        comments.traverse(comment =>
          XorT[F, Throwable, User](alg.getUser(comment.user))
            .map((comment, _))
        ).map((issue, _))
      }: XorT[F, Throwable, List[(Issue, List[(Comment, User)])]]
    } yield users
      ).value
  }

}
