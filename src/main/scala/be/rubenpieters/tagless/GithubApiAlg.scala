package be.rubenpieters.tagless

import be.rubenpieters.free.{IssueNumber, Owner, Repo}
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import be.rubenpieters.tagless.GithubApi.GithubApiResult

/**
  * Created by ruben on 23/10/2016.
  */
trait GithubApiAlg[F[_]] {
  def listIssues(owner: Owner, repo: Repo): F[GithubApiResult[List[Issue]]]
  def getComments(owner: Owner, repo: Repo, issue: IssueNumber): F[GithubApiResult[List[Comment]]]
  def getUser(userRef: UserReference): F[GithubApiResult[User]]
}