package be.rubenpieters.main


import cats.implicits._
import java.util.concurrent.{Executor, Executors}

import be.rubenpieters.free.{GithubFutureInterpreter, GithubOps}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * Created by ruben on 23/10/2016.
  */
object FreeMain {
  def main(args: Array[String]) = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))

    val future = new GithubOps().listIssues("typelevel", "cats").foldMap(new GithubFutureInterpreter)
    val futureResult = Await.result(future, 1.minute)
    println(futureResult)
  }
}
