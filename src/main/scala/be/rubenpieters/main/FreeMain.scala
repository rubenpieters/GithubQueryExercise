package be.rubenpieters.main


import cats.implicits._
import java.util.concurrent.{Executor, Executors}

import be.rubenpieters.free.{GithubFutureInterpreter, GithubOps, TestGithubFutureInterpreter}
import be.rubenpieters.model.github.{Comment, Issue, User, UserReference}

import scala.concurrent.{Await, ExecutionContext}
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