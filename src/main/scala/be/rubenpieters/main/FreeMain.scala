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
    val interpreterApp = new GithubOps().interpretAppOpt(interpreterFree)

    val future = new GithubOps().runCop(new GithubOps().getCommentsCop("typelevel", "cats", 1), interpreterFree, interpreterApp)
    val futureResult = Await.result(future, 1.minute)
    println(futureResult)
  }
}

