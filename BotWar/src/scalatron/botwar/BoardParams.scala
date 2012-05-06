/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar


/** Constant parameters describing the size and desired content of a game board.
 */
object BoardParams {
    sealed trait Perimeter
    object Perimeter {
        case object None extends Perimeter      // no perimeter wall
        case object Open extends Perimeter      // perimeter wall with breaks
        case object Closed extends Perimeter    // fully enclosing perimeter wall
    }
}
case class BoardParams(
    size: XY,
    perimeter: BoardParams.Perimeter,
    wallCount: Int,
    goodPlantCount: Int,
    badPlantCount: Int,
    goodBeastCount: Int,
    badBeastCount: Int
    )

