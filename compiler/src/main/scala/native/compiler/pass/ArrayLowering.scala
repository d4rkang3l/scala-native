package native
package compiler
package pass

import native.nir._

/** Eliminates:
 *  - Type.ArrayClass
 *  - Op.{ArrAlloc, ArrLength, ArrElem}
 */
class ArrayLowering(implicit fresh: Fresh) extends Pass {
  private val i8_* = Type.Ptr(Type.I8)

  override def preInst = {
    case Inst(n, Op.ArrAlloc(ty, len)) =>
      val arrty = Intr.array_type.get(ty).getOrElse(Intr.object_array_type)
      val size  = Val.Local(fresh(), Type.Size)
      Seq(
        Inst(size.name, Op.ArrSizeOf(ty, len)),
        Inst(n,         Intr.call(Intr.alloc, arrty, size))
      )

    case Inst(n, Op.ArrLength(arr)) =>
      val arrptr = Type.Ptr(Intr.object_array)
      val cast   = Val.Local(fresh(), arrptr)
      val elem   = Val.Local(fresh(), Type.Ptr(Type.I32))
      Seq(
        Inst(cast.name, Op.Conv(Conv.Bitcast, arrptr, arr)),
        Inst(elem.name, Op.Elem(Type.I32, cast, Seq(Val.I32(0), Val.I32(1)))),
        Inst(n,         Op.Load(Type.I32, elem))
      )

    case Inst(n, Op.ArrElem(ty, arr, idx)) =>
      val arrptr = Type.Ptr(Intr.array.get(ty).getOrElse(Intr.object_array))
      val cast   = Val.Local(fresh(), arrptr)
      Seq(
        Inst(cast.name, Op.Conv(Conv.Bitcast, arrptr, arr)),
        Inst(n,         Op.Elem(ty, cast, Seq(Val.I32(0), Val.I32(2), idx)))
      )
  }

  override def preType = {
    case Type.ArrayClass(_) => i8_*
  }
}
