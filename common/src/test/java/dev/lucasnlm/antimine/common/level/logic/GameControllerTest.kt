package dev.lucasnlm.antimine.common.level.logic

import dev.lucasnlm.antimine.common.level.GameController
import dev.lucasnlm.antimine.core.models.Area
import dev.lucasnlm.antimine.core.models.Difficulty
import dev.lucasnlm.antimine.core.models.Score
import dev.lucasnlm.antimine.preferences.models.ControlStyle
import dev.lucasnlm.antimine.preferences.models.GameControl
import dev.lucasnlm.antimine.preferences.models.Minefield
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class GameControllerTest {
    private suspend fun withGameController(
        clickOnCreate: Boolean = true,
        block: suspend (GameController) -> Unit,
    ) {
        val minefield = Minefield(10, 10, 20)
        val gameController = GameController(
            minefield = minefield,
            seed = 200L,
            useSimonTatham = false,
            difficulty = Difficulty.Standard,
        )
        if (clickOnCreate) {
            runTest(UnconfinedTestDispatcher()) {
                fakeSingleClick(gameController, 10)
            }
        }
        block(gameController)
    }

    @Test
    fun testGetMinesCount() = runTest(UnconfinedTestDispatcher()) {
        withGameController {
            assertEquals(20, it.getMinesCount())
        }
    }

    @Test
    fun testGetScore() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertEquals(Score(20, 20, 100), controller.getScore())

            repeat(20) { markedMines ->
                controller
                    .mines()
                    .take(markedMines)
                    .filter { it.mark.isNone() }
                    .forEach {
                        // Put a flag.
                        fakeLongPress(controller, it.id)
                    }

                assertEquals(markedMines, controller.rightFlags())
            }
        }
    }

    @Test
    fun testFlagAllMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            val minesCount = controller.mines().count()

            controller.flagAllMines()
            val actualFlaggedMines = controller.mines().count { it.isCovered && it.mark.isFlag() }

            assertEquals(minesCount, actualFlaggedMines)
        }
    }

    @Test
    fun testFindExplodedMine() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertNull(controller.findExplodedMine())
            val target = controller.mines().first()
            fakeSingleClick(controller, target.id)
            assertNotNull(controller.findExplodedMine())
            assertEquals(target.id, controller.findExplodedMine()!!.id)
        }
    }

    @Test
    fun testTakeExplosionRadius() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            val lastMine = controller.mines().last()
            assertEquals(
                listOf(95, 85, 74, 73, 65, 88, 55, 91, 45, 52, 90, 47, 59, 42, 36, 32, 39, 28, 4, 3),
                controller.takeExplosionRadius(lastMine).map { it.id }.toList(),
            )

            val firstMine = controller.mines().first()
            assertEquals(
                listOf(3, 4, 32, 42, 36, 45, 52, 28, 55, 47, 65, 39, 73, 74, 59, 85, 91, 95, 88, 90),
                controller.takeExplosionRadius(firstMine).map { it.id }.toList(),
            )

            val midMine = controller.mines().take(controller.getMinesCount() / 2).last()
            assertEquals(
                listOf(52, 42, 32, 73, 74, 55, 45, 65, 91, 85, 36, 90, 95, 3, 47, 4, 28, 88, 59, 39),
                controller.takeExplosionRadius(midMine).map { it.id }.toList(),
            )
        }
    }

    @Test
    fun testShowAllMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.showAllMistakes()
            controller.mines().filter { it.mistake }.forEach {
                assertEquals(it.isCovered, false)
            }
            controller.mines().filter { it.mark.isFlag() }.forEach {
                assertEquals(it.isCovered, true)
            }
        }
    }

    @Test
    fun testShowWrongFlags() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.field().first { !it.hasMine && it.isCovered }.run {
                fakeLongPress(controller, id)
            }

            controller.mines().first().run {
                fakeLongPress(controller, id)
            }

            controller.showWrongFlags()

            val wrongFlag = controller.field().first { !it.hasMine && it.isCovered }
            val rightFlag = controller.mines().first()

            assertTrue(wrongFlag.mistake)
            assertFalse(rightFlag.mistake)
        }
    }

    @Test
    fun testRevealAllEmptyAreas() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            val covered = controller.field { it.isCovered }
            assertTrue(covered.isNotEmpty())
            controller.revealAllEmptyAreas()
            assertEquals(controller.mines(), controller.field { it.isCovered })
        }
    }

    @Test
    fun testFlaggedAllMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.hasFlaggedAllMines())

            controller.field()
                .filter { it.hasMine }
                .take(10)
                .forEach { fakeLongPress(controller, it.id) }

            assertFalse(controller.hasFlaggedAllMines())

            controller.field()
                .filter { it.hasMine }
                .filter { it.mark.isNone() }
                .forEach { fakeLongPress(controller, it.id) }

            assertTrue(controller.hasFlaggedAllMines())
        }
    }

    @Test
    fun testRemainingMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertEquals(20, controller.remainingMines())

            repeat(20) { flagCount ->
                controller.field()
                    .filter { it.hasMine }
                    .take(flagCount)
                    .forEach {
                        if (!it.mark.isFlag()) {
                            fakeLongPress(controller, it.id)
                        }
                    }
                assertEquals("flagging $flagCount mines", 20 - flagCount, controller.remainingMines())
            }
        }
    }

    @Test
    fun testHasIsolatedAllMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.hasIsolatedAllMines())
            assertFalse(controller.isGameOver())

            val lastArea = controller.field()
                .last { !it.hasMine }.id

            controller.field()
                .filter { !it.hasMine }
                .onEach {
                    fakeSingleClick(controller, it.id)

                    if (it.id != lastArea) {
                        assertFalse(controller.hasIsolatedAllMines())
                        assertFalse(controller.isGameOver())
                    } else {
                        assertTrue(controller.hasIsolatedAllMines())
                        assertTrue(controller.isGameOver())
                    }
                }
        }
    }

    @Test
    fun testHasAnyMineExploded() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.hasAnyMineExploded())

            controller.field().first { it.hasMine }.also { fakeSingleClick(controller, it.id) }

            assertTrue(controller.hasAnyMineExploded())
        }
    }

    @Test
    fun testGameOverWithMineExploded() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.isGameOver())

            controller.field().first { it.hasMine }.also { fakeSingleClick(controller, it.id) }

            assertTrue(controller.isGameOver())
        }
    }

    @Test
    fun testVictory() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.isVictory())

            controller.mines().forEach { fakeLongPress(controller, it.id) }
            assertFalse(controller.isVictory())

            controller.field { !it.hasMine }.forEach { fakeSingleClick(controller, it.id) }
            assertTrue(controller.isVictory())

            controller.mines().first().run {
                fakeSingleClick(controller, id)
                fakeSingleClick(controller, id)
            }
            assertFalse(controller.isVictory())
        }
    }

    @Test
    fun testCantShowVictoryIfHasNoMines() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            assertFalse(controller.isVictory())
        }
    }

    @Test
    fun testControlFirstActionWithStandard() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.updateGameControl(GameControl.fromControlType(ControlStyle.Standard))
            assertTrue(controller.at(3).isCovered)
            fakeSingleClick(controller, 3)
            assertFalse(controller.at(3).isCovered)
        }
    }

    @Test
    fun testControlSecondActionWithStandard() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.Standard))
                useQuestionMark(true)

                fakeLongPress(controller, 4)
                assertTrue(at(4).mark.isFlag())
                assertTrue(at(4).isCovered)
                fakeLongPress(controller, 4)
                assertTrue(at(4).mark.isQuestion())
                assertTrue(at(4).isCovered)
                fakeLongPress(controller, 4)
                assertTrue(at(4).mark.isNone())
                assertTrue(at(4).isCovered)

                useQuestionMark(false)
                fakeLongPress(controller, 4)
                assertTrue(at(4).mark.isFlag())
                assertTrue(at(4).isCovered)
                fakeLongPress(controller, 4)
                assertTrue(at(4).mark.isNone())
                assertTrue(at(4).isCovered)
            }
        }
    }

    @Test
    fun testControlStandardOpenMultiple() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.Standard))
                fakeSingleClick(controller, 14)
                assertFalse(at(14).isCovered)

                field().filterNeighborsOf(at(14)).forEach {
                    assertTrue(it.isCovered)
                }

                mines().forEach { fakeLongPress(controller, it.id) }

                mines().forEach { assertTrue(it.mark.isFlag()) }

                fakeLongPress(controller, 14)

                field().filterNeighborsOf(at(14)).forEach {
                    if (it.hasMine) {
                        assertTrue(it.isCovered)
                    } else {
                        assertFalse(it.isCovered)
                    }
                }
            }
        }
    }

    @Test
    fun testControlFirstActionWithFastFlag() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.FastFlag))
                fakeSingleClick(controller, 3)
                assertTrue(at(3).isCovered)
                assertTrue(at(3).mark.isFlag())
                fakeLongPress(controller, 3)
                assertFalse(at(3).mark.isFlag())
                assertTrue(at(3).isCovered)
                fakeLongPress(controller, 3)
                assertFalse(at(3).isCovered)
            }
        }
    }

    @Test
    fun testControlFirstActionWithInvertedDoubleClick() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClickInverted))
                assertTrue(at(3).isCovered)
                fakeDoubleClick(controller, 3)
                assertTrue(at(3).isCovered)
                assertTrue(at(3).mark.isFlag())
                fakeDoubleClick(controller, 3)
                assertFalse(at(3).mark.isFlag())
                assertTrue(at(3).isCovered)
            }
        }
    }

    @Test
    fun testControlSecondActionWithInvertedDoubleClick() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClickInverted))
                assertTrue(at(3).isCovered)
                fakeSingleClick(controller, 3)
                assertFalse(at(3).isCovered)
            }
        }
    }

    @Test
    fun testControlFastFlagOpenMultiple() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.FastFlag))
                fakeLongPress(controller, 14)
                assertFalse(at(14).isCovered)

                field().filterNeighborsOf(at(14)).forEach {
                    assertTrue(it.isCovered)
                }

                mines().forEach {
                    fakeSingleClick(controller, it.id)
                }

                mines().forEach {
                    assertTrue(it.mark.isFlag())
                }

                fakeSingleClick(controller, 14)
                field().filterNeighborsOf(at(14)).forEach {
                    if (it.hasMine) {
                        assertTrue(it.isCovered)
                    } else {
                        assertFalse(it.isCovered)
                    }
                }
            }
        }
    }

    @Test
    fun testControlFirstActionWithDoubleClick() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClick))
                fakeSingleClick(controller, 3)
                assertTrue(at(3).isCovered)
                assertTrue(at(3).mark.isFlag())
                fakeDoubleClick(controller, 3)
                assertFalse(at(3).mark.isFlag())
                assertTrue(at(3).isCovered)
                fakeDoubleClick(controller, 3)
                assertFalse(at(3).isCovered)
            }
        }
    }

    @Test
    fun testControlFirstActionWithDoubleClickAndWithoutQuestionMark() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClick))

                useQuestionMark(true)
                var targetId = 4
                fakeSingleClick(controller, targetId)
                assertTrue(at(targetId).isCovered)
                assertTrue(at(targetId).mark.isFlag())
                fakeSingleClick(controller, targetId)
                assertTrue(at(targetId).mark.isQuestion())
                assertTrue(at(targetId).isCovered)
                fakeSingleClick(controller, targetId)
                assertTrue(at(targetId).mark.isNone())
                assertTrue(at(targetId).isCovered)
                fakeDoubleClick(controller, targetId)
                assertFalse(at(targetId).isCovered)

                useQuestionMark(false)
                targetId = 3
                fakeSingleClick(controller, targetId)
                assertTrue(at(targetId).isCovered)
                assertTrue(at(targetId).mark.isFlag())
                fakeSingleClick(controller, targetId)
                assertFalse(at(targetId).mark.isFlag())
                assertTrue(at(targetId).isCovered)
                fakeDoubleClick(controller, targetId)
                assertFalse(at(targetId).isCovered)
            }
        }
    }

    @Test
    fun testControlDoubleClickOpenMultiple() = runTest(UnconfinedTestDispatcher()) {
        withGameController { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClick))
                fakeDoubleClick(controller, 14)
                assertFalse(at(14).isCovered)
                field().filterNeighborsOf(at(14)).forEach {
                    assertTrue(it.isCovered)
                }

                mines().forEach { fakeSingleClick(controller, it.id) }
                mines().forEach { assertTrue(it.mark.isFlag()) }

                fakeSingleClick(controller, 14)

                field().filterNeighborsOf(at(14)).forEach {
                    val message = if (it.hasMine) {
                        "${it.id} has mine, so it must be covered after open multiple"
                    } else {
                        "${it.id} doesn\'t have mine, so it must be uncovered after open multiple"
                    }
                    assertTrue(message, it.hasMine == it.isCovered)
                }
            }
        }
    }

    @Test
    fun testIfDoubleClickPlantMinesOnFirstClick() = runTest(UnconfinedTestDispatcher()) {
        withGameController(clickOnCreate = false) { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.DoubleClick))
                assertFalse(hasMines())
                assertEquals(0, field().filterNot { it.isCovered }.count())
                fakeSingleClick(controller, 40)
                assertTrue(hasMines())
                field().filterNeighborsOf(at(40)).forEach { assertFalse(it.isCovered) }
            }
        }
    }

    @Test
    fun testIfFastFlagPlantMinesOnFirstClick() = runTest(UnconfinedTestDispatcher()) {
        withGameController(clickOnCreate = false) { controller ->
            controller.run {
                updateGameControl(GameControl.fromControlType(ControlStyle.FastFlag))
                assertFalse(hasMines())
                assertEquals(0, field().filterNot { it.isCovered }.count())
                fakeSingleClick(controller, 40)
                assertTrue(hasMines())
                field().filterNeighborsOf(at(40)).forEach { assertFalse(it.isCovered) }
            }
        }
    }

    private fun GameController.at(index: Int): Area {
        return this.field().first { it.id == index }
    }

    private suspend fun fakeSingleClick(gameController: GameController, index: Int) {
        gameController.singleClick(index).collect()
    }

    private suspend fun fakeLongPress(gameController: GameController, index: Int) {
        gameController.longPress(index).collect()
    }

    private suspend fun fakeDoubleClick(gameController: GameController, index: Int) {
        gameController.doubleClick(index).collect()
    }
}
