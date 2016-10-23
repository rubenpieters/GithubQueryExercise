package be.rubenpieters.free

import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.XorT
import cats.free.Free
import cats.implicits._

/**
  * Created by ruben on 23/10/2016.
  */
class GithubOps {
  type GithubOpsFree[A] = Free[GithubApiDsl, A]

  def listIssues(owner: Owner, repo: Repo): GithubOpsFree[GithubApiDslResult[List[Issue]]] =
    Free.liftF(ListIssues(owner, repo))

  def getComments(owner: Owner, repo: Repo, issue: Issue): GithubOpsFree[GithubApiDslResult[List[Comment]]] =
    Free.liftF(GetComments(owner, repo, issue))

  def getUser(userRef: UserReference): GithubOpsFree[GithubApiDslResult[User]] =
    Free.liftF(GetUser(userRef))

  def allUsers(owner: Owner, repo: Repo)
  : GithubOpsFree[GithubApiDslResult[List[(Issue, List[(Comment, User)])]]] = (for {
    issues <- XorT[GithubOpsFree, Throwable, List[Issue]](
      listIssues(owner, repo))

    issueComments <-
      issues.traverseU(issue =>
        XorT[GithubOpsFree, Throwable, List[Comment]](getComments(owner, repo, issue))
          .map((issue, _))
      ): XorT[GithubOpsFree, Throwable, List[(Issue, List[Comment])]]

    users <-
      issueComments.traverseU { case (issue, comments) =>
        comments.traverseU(comment =>
          XorT[GithubOpsFree, Throwable, User](getUser(comment.user))
            .map((comment, _))
        ).map((issue, _))
      }: XorT[GithubOpsFree, Throwable, List[(Issue, List[(Comment, User)])]]
  } yield users
    ).value
}
