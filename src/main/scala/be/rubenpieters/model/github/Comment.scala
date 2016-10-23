package be.rubenpieters.model.github

/**
  * Created by ruben on 23/10/2016.
  */
case class Comment(
                    id: Long
                    , body: String
                    , user: UserReference
                  )
