package be.rubenpieters.model.github

import be.rubenpieters.free.IssueNumber

/**
  * Created by ruben on 23/10/2016.
  */
case class Issue(
                  id: Long
                  , number: IssueNumber
                  , title: String
                  , user: UserReference
                ) {

}
