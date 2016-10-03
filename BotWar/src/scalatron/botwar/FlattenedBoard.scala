/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar



/** A "flattened" view of the game board: instead of maps holding entities, this view holds an
  * array of cells with (optional) references to entities from each cell. The flattened board
  * is computed once each cycle and then cached to accelerate the computation of occluded and
  * unoccluded partial game board views for the bots.
  */
case class FlattenedBoard(boardSize: XY, cells: Array[Option[Bot]]) {
    val centerAbsPos = XY(boardSize.x/2, boardSize.y/2)

    def wrap(pos: XY) = pos.wrap(boardSize)

    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * boardSize.x
    def indexFromAbsPos(x: Int, y: Int) = x + y * boardSize.x
    def relPosFromAbsPos(absPos: XY) = XY(absPos.x-centerAbsPos.x, absPos.y-centerAbsPos.y)


    /** Returned area is centered on 'center' and has edges with lengths size*2 + 1. */
    def computeView(center: XY, halfSize: Int) : FlattenedBoard = {
        val corner = XY(center.x - halfSize, center.y - halfSize)
        val edgeCellCount = halfSize*2+1
        val extractedCells = Array.fill[Option[Bot]](edgeCellCount * edgeCellCount)(None)
        for(x <- 0 until edgeCellCount) {
            for(y <- 0 until edgeCellCount) {
                val rawPos = corner + XY(x,y)
                val wrappedPos = wrap( rawPos )
                val sourceIndex = wrappedPos.x + wrappedPos.y * boardSize.x
                val targetIndex = y * edgeCellCount + x
                extractedCells(targetIndex) = cells(sourceIndex)
            }
        }
        FlattenedBoard(XY(edgeCellCount, edgeCellCount), extractedCells)
    }


    /** Performs an in-situ occlusion of the view. Works by, for each edge cell, walking outward
      * to it from the center (the bot's position) and labeling cells encountered after hitting
      * a wall as 'occluded'. */
    def occlude(): Unit = {

        /*
        function line(x0, y0, x1, y1)
           dx := abs(x1-x0)
           dy := abs(y1-y0)
           if x0 < x1 then sx := 1 else sx := -1
           if y0 < y1 then sy := 1 else sy := -1
           err := dx-dy

           loop
             setPixel(x0,y0)
             if x0 = x1 and y0 = y1 exit loop
             e2 := 2*err
             if e2 > -dy then
               err := err - dy
               x0 := x0 + sx
             end if
             if e2 <  dx then
               err := err + dx
               y0 := y0 + sy
             end if
           end loop
         */
        def walkOutwardTo(targetRelPos: XY): Unit = {
            // see http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
            var x0 = 0; var y0 = 0
            var x1 = targetRelPos.x; var y1 = targetRelPos.y

            val dx = (x1-x0).abs
            val dy = (y1-y0).abs
            val sx = if(x0 < x1) 1 else -1
            val sy = if(y0 < y1) 1 else -1

            var err = dx-dy
            var stepping = true
            var occluded = false
            var traversingWall = false     // to prevent a wall from occluding a neighboring wall
            while(stepping) {
                val cellIndex = indexFromAbsPos(centerAbsPos.x + x0, centerAbsPos.y + y0)
                if(occluded) {
                    if(traversingWall) {
                        val cellIsWall = cells(cellIndex) match {
                            case None => false
                            case Some(bot) => bot.isWall
                        }
                        if(!cellIsWall) {
                            cells(cellIndex) = Bot.OccludedOpt
                            traversingWall = false
                        }
                    } else {
                        cells(cellIndex) = Bot.OccludedOpt
                    }
                } else {
                    cells(cellIndex) match {
                        case None =>
                        case Some(bot) =>
                            if(bot.isWall) {
                                traversingWall = true
                                occluded = true
                            }
                    }
                }

                if(x0==x1 && y0==y1) return

                val e2 = 2*err
                if(e2 > -dy) {
                    err -= dy
                    x0 += sx
                }
                if(e2 < dx) {
                    err += dx
                    y0 += sy
               }
            }
        }

        // iterate over all edge cells - horizontally top & bottom
        for(x <- 0 until boardSize.x) {
            val absPos1 = XY(x,0)
            val relPos1 = relPosFromAbsPos(absPos1)
            walkOutwardTo(relPos1)

            val absPos2 = XY(x,boardSize.y-1)
            val relPos2 = relPosFromAbsPos(absPos2)
            walkOutwardTo(relPos2)
        }

        // iterate over all edge cells - vertically left & right
        for(y <- 1 until (boardSize.y-1)) {
            val absPos1 = XY(0,y)
            val relPos1 = relPosFromAbsPos(absPos1)
            walkOutwardTo(relPos1)

            val absPos2 = XY(boardSize.x-1,y)
            val relPos2 = relPosFromAbsPos(absPos2)
            walkOutwardTo(relPos2)
        }
    }
}
