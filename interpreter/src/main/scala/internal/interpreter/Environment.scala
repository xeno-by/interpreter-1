package scala.meta
package internal
package interpreter

import scala.meta.interpreter.{Env, EnvImpl}
import scala.meta.internal.representations._

/**
 * Created by rutz on 03/11/15.
 */

final class Environment(stack: CallStack) extends Env {
  def this() = this(List[Frame](Map[Slot, Value]()))

  // Inherited from interpreter.Env
  def +(name: String, value: Any): Environment = ???//this + (Local(name), Val(value))

  // Usable only in Environment
  def apply(name: Slot): Value = stack.head(name)
  def +(name: Slot, value: Value): Environment = new Environment((stack.head + (name -> value)) :: stack.tail)
  def push(frame: Frame): Environment = new Environment(frame :: stack)
  def pop: (Frame, Environment) = (stack.head, new Environment(stack.tail))
  def get: Frame = stack.head
  def contains(name: Slot) = stack.exists(_.contains(name))

  override def toString = s"""Env(${this.get.mkString("\n")})"""
}

object Environment {
  def apply(env: EnvImpl, stat: Tree)(implicit ctx: Context) = {
    val names = scala.collection.mutable.SortedSet[String]()
    val toBuildEnvironment = (stat collect {
       // Check if there is a definition for name in the Tree and if it is not already there as well
      case name: Term.Name if env.slots.isDefinedAt(name.toString) && !names.contains(name.toString) =>
        names += name.toString
        Local(name) -> Val(env(name.toString))
    }).toMap[Slot, Value]
    new Environment(List[Frame](toBuildEnvironment))
  }
}