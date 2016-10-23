package be.rubenpieters.free

import cats.free.Free

/**
  * Created by ruben on 23/10/2016.
  */
class GithubOps {
  type GithubOpsFree[A] = Free[GithubApiDsl, A]

  def listIssues(owner: Owner, repo: Repo): GithubOpsFree[List[Issue]] =
    Free.liftF(ListIssues(owner, repo))

  def getComments(owner: Owner, repo: Repo, issue: Issue): GithubOpsFree[List[Comment]] =
    Free.liftF(GetComments(owner, repo, issue))

  def getUser(login: UserLogin): GithubOpsFree[User] =
    Free.liftF(GetUser(login))

}
