package dev.lucasnlm.antimine.preferences.models

/**
 * Possible action response to an user action.
 */
enum class ActionResponse {
    OpenTile,
    SwitchMark,
    OpenNeighbors,
    OpenOrMark,
}

/**
 * [Actions] links an [ActionResponse] to an user action.
 */
data class Actions(
    val singleClick: ActionResponse?,
    val doubleClick: ActionResponse?,
    val longPress: ActionResponse?,
)

/**
 * These are the current available game control styles.
 * Check [GameControl] to details.
 */
enum class ControlStyle {
    Standard,
    DoubleClick,
    FastFlag,
    DoubleClickInverted,
    SwitchMarkOpen,
}

/**
 * [GameControl] will map an user action (from [Actions]) to an [ActionResponse].
 * This is necessary because same users rather that single click open the tile, other that it flags the tile.
 */
sealed class GameControl(
    val id: ControlStyle,
    val onCovered: Actions,
    val onUncovered: Actions,
) {
    object Standard : GameControl(
        id = ControlStyle.Standard,
        onCovered = Actions(
            singleClick = ActionResponse.OpenTile,
            longPress = ActionResponse.SwitchMark,
            doubleClick = null,
        ),
        onUncovered = Actions(
            singleClick = null,
            longPress = ActionResponse.OpenNeighbors,
            doubleClick = null,
        ),
    )

    object FastFlag : GameControl(
        id = ControlStyle.FastFlag,
        onCovered = Actions(
            singleClick = ActionResponse.SwitchMark,
            longPress = ActionResponse.OpenTile,
            doubleClick = null,
        ),
        onUncovered = Actions(
            singleClick = ActionResponse.OpenNeighbors,
            longPress = null,
            doubleClick = null,
        ),
    )

    object DoubleClick : GameControl(
        id = ControlStyle.DoubleClick,
        onCovered = Actions(
            singleClick = ActionResponse.SwitchMark,
            longPress = null,
            doubleClick = ActionResponse.OpenTile,
        ),
        onUncovered = Actions(
            singleClick = ActionResponse.OpenNeighbors,
            longPress = null,
            doubleClick = null,
        ),
    )

    object DoubleClickInverted : GameControl(
        id = ControlStyle.DoubleClickInverted,
        onCovered = Actions(
            singleClick = ActionResponse.OpenTile,
            longPress = null,
            doubleClick = ActionResponse.SwitchMark,
        ),
        onUncovered = Actions(
            singleClick = ActionResponse.OpenNeighbors,
            longPress = null,
            doubleClick = null,
        ),
    )

    object SwitchMarkOpen : GameControl(
        id = ControlStyle.SwitchMarkOpen,
        onCovered = Actions(
            singleClick = ActionResponse.OpenOrMark,
            longPress = null,
            doubleClick = null,
        ),
        onUncovered = Actions(
            singleClick = ActionResponse.OpenNeighbors,
            longPress = null,
            doubleClick = null,
        ),
    )

    companion object {
        fun fromControlType(controlStyle: ControlStyle): GameControl {
            return when (controlStyle) {
                ControlStyle.Standard -> Standard
                ControlStyle.DoubleClick -> DoubleClick
                ControlStyle.FastFlag -> FastFlag
                ControlStyle.DoubleClickInverted -> DoubleClickInverted
                ControlStyle.SwitchMarkOpen -> SwitchMarkOpen
            }
        }
    }
}
