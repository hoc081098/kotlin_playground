package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either

data class User(val id: Int, val name: String)
sealed interface Error {
  data class UserNotFound(val id: Int) : Error
}

fun findUserById(id: Int): User =
  throw NoSuchElementException("User'$id' not found")

context(Raise<Error.UserNotFound>) fun findUserByIdWithRaise(id: Int): User =
  catch(
    block = { findUserById(id) },
    catch = {
      if (it is NoSuchElementException)
        raise(Error.UserNotFound(id))
      else
        throw it
    }
  )

fun main() {
  println(either { findUserByIdWithRaise(1) })
}
