package scalatron.botwar

import org.testng.annotations.Test
import org.testng.Assert


class XYTest {
    @Test def test_toDirection45() {
        Assert.assertEquals(XY.Right.toDirection45, Direction45.Right)
        Assert.assertEquals(XY.RightUp.toDirection45, Direction45.RightUp)
        Assert.assertEquals(XY.Up.toDirection45, Direction45.Up)
        Assert.assertEquals(XY.UpLeft.toDirection45, Direction45.UpLeft)
        Assert.assertEquals(XY.Left.toDirection45, Direction45.Left)
        Assert.assertEquals(XY.LeftDown.toDirection45, Direction45.LeftDown)
        Assert.assertEquals(XY.Down.toDirection45, Direction45.Down)
        Assert.assertEquals(XY.DownRight.toDirection45, Direction45.DownRight)
    }

    @Test def test_rotateCounterClockwise45() {
        Assert.assertEquals(XY.Right.rotateCounterClockwise45, XY.RightUp)
        Assert.assertEquals(XY.RightUp.rotateCounterClockwise45, XY.Up)
        Assert.assertEquals(XY.Up.rotateCounterClockwise45, XY.UpLeft)
        Assert.assertEquals(XY.UpLeft.rotateCounterClockwise45, XY.Left)
        Assert.assertEquals(XY.Left.rotateCounterClockwise45, XY.LeftDown)
        Assert.assertEquals(XY.LeftDown.rotateCounterClockwise45, XY.Down)
        Assert.assertEquals(XY.Down.rotateCounterClockwise45, XY.DownRight)
        Assert.assertEquals(XY.DownRight.rotateCounterClockwise45, XY.Right)
    }

    @Test def test_rotateCounterClockwise90() {
        Assert.assertEquals(XY.Right.rotateCounterClockwise90, XY.Up)
        Assert.assertEquals(XY.Up.rotateCounterClockwise90, XY.Left)
        Assert.assertEquals(XY.Left.rotateCounterClockwise90, XY.Down)
        Assert.assertEquals(XY.Down.rotateCounterClockwise90, XY.Right)

        Assert.assertEquals(XY.RightUp.rotateCounterClockwise90, XY.UpLeft)
        Assert.assertEquals(XY.UpLeft.rotateCounterClockwise90, XY.LeftDown)
        Assert.assertEquals(XY.LeftDown.rotateCounterClockwise90, XY.DownRight)
        Assert.assertEquals(XY.DownRight.rotateCounterClockwise90, XY.RightUp)
    }

    @Test def test_rotateClockwise45() {
        Assert.assertEquals(XY.Right.rotateClockwise45, XY.DownRight)
        Assert.assertEquals(XY.DownRight.rotateClockwise45, XY.Down)
        Assert.assertEquals(XY.Down.rotateClockwise45, XY.LeftDown)
        Assert.assertEquals(XY.LeftDown.rotateClockwise45, XY.Left)
        Assert.assertEquals(XY.Left.rotateClockwise45, XY.UpLeft)
        Assert.assertEquals(XY.UpLeft.rotateClockwise45, XY.Up)
        Assert.assertEquals(XY.Up.rotateClockwise45, XY.RightUp)
        Assert.assertEquals(XY.RightUp.rotateClockwise45, XY.Right)
    }
}