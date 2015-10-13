package interpreter

/**
 * Created by rutz on 05/10/15.
 */

final class Environment(stack: CallStack) {
  def this() = this(List[Frame]())

  def apply(name: Slot): Value = stack.head(name)
  def +(name: Slot, value: Value): Environment = new Environment((stack.head + (name -> value)) :: stack.tail)
  def push(frame: Frame): Environment = new Environment(frame :: stack)
  def pop: (Frame, Environment) = (stack.head, new Environment(stack.tail))
}