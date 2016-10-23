package be.rubenpieters.main


import cats.implicits._
import java.util.concurrent.{Executor, Executors}

import be.rubenpieters.free.{GithubFutureInterpreter, GithubOps, TestGithubFutureInterpreter}
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}
import cats.data.Coproduct
import cats.~>

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by ruben on 23/10/2016.
  */
object FreeMain {
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val future = new GithubOps().allUsers("typelevel", "cats").foldMap(new GithubFutureInterpreter)
    val futureResult = Await.result(future, 1.minute)
    println(futureResult)
  }
}

object FreeSimulatedMain {
  import SimulatedData._
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val future = new GithubOps().allUsers("typelevel", "cats").foldMap(new TestGithubFutureInterpreter(issues, comments, users))
    val futureResult = Await.result(future, 1.minute)
    println(futureResult)
  }
}

object FreeApplicativeSimulatedMain {
  import SimulatedData._
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val interpreter = new TestGithubFutureInterpreter(issues, comments, users)

    val futureIssues = new GithubOps().listIssuesApp("typelevel", "cats").foldMap(interpreter)
    val futureIssuesResult = Await.result(futureIssues, 1.minute)

    val futureComments = new GithubOps().getCommentsByListOfIssue("typelevel", "cats", futureIssuesResult.getOrElse(sys.error("error!"))).foldMap(interpreter)
    val futureCommentsResult = Await.result(futureComments, 1.minute)

    val futureUsers = new GithubOps().addUserInfo(futureCommentsResult).foldMap(interpreter)
    val futureUsersResult = Await.result(futureUsers, 1.minute)
    println(futureUsersResult)
  }
}

object FreeApplicativeSimulatedMainNoDuplicateUsers {
  import SimulatedData._
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val interpreter = new TestGithubFutureInterpreter(issues, comments, users)

    val futureIssues = new GithubOps().listIssuesApp("typelevel", "cats").foldMap(interpreter)
    val futureIssuesResult = Await.result(futureIssues, 1.minute)

    val futureComments = new GithubOps().getCommentsByListOfIssue("typelevel", "cats", futureIssuesResult.getOrElse(sys.error("error!"))).foldMap(interpreter)
    val futureCommentsResult = Await.result(futureComments, 1.minute)

    val futureUsers = new GithubOps().interpretOpt(new GithubOps().addUserInfo(futureCommentsResult), interpreter)

    val futureUsersResult = Await.result(futureUsers, 1.minute)
    println(futureUsersResult)
  }
}

object CoproductSimulatedMainNoDuplicateUsers {
  import SimulatedData._
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val interpreterFree = new TestGithubFutureInterpreter(issues, comments, users)
    val mapping: Map[UserReference, User] = Map.empty
    val interpreterApp = new GithubOps().optimizeNat(mapping, interpreterFree)

//    new GithubOps().mixedInterpreter(interpreterFree, interpreterApp)


  }
}

object SimulatedData {
  val issues = Map(
    ("typelevel", "cats") ->
      ((1 to 5).map(x => Issue(x, x, s"issue$x", UserReference(1))) ++
        (6 to 10).map(x => Issue(x, x, s"issue$x", UserReference(2)))
        ).toList
  )

  val comments = (1L to 10L).map { x =>
    ("typelevel", "cats", x) -> List(
      Comment(x, "body", UserReference(1))
    )
  }.toMap

  val users = Map(
    UserReference(1) -> User(1, "user1")
    ,UserReference(2) -> User(2, "user2")
  )
}