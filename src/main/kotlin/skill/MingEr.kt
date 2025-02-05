package com.fengsheng.skill

import com.fengsheng.*
import com.fengsheng.protos.Common.color.Blue
import com.fengsheng.protos.Common.color.Red
import com.fengsheng.protos.skillMingErToc
import org.apache.logging.log4j.kotlin.logger

/**
 * 老鳖技能【明饵】：你传出的红色或蓝色情报被接收后，你和接收者各摸一张牌。
 */
class MingEr : TriggeredSkill {
    override val skillId = SkillId.MING_ER

    override val isInitialSkill = true

    override fun execute(g: Game, askWhom: Player): ResolveResult? {
        val event = g.findEvent<ReceiveCardEvent>(this) { event ->
            askWhom === event.sender || return@findEvent false
            event.sender.getSkillUseCount(skillId) == 0 || return@findEvent false
            val colors = event.messageCard.colors
            Red in colors || Blue in colors
        } ?: return null
        logger.info("${event.sender}发动了[明饵]")
        g.players.send { skillMingErToc { playerId = it.getAlternativeLocation(event.sender.location) } }
        if (event.sender === event.inFrontOfWhom)
            event.sender.draw(2)
        else
            g.sortedFrom(listOf(event.sender, event.inFrontOfWhom), event.whoseTurn.location).forEach { it.draw(1) }
        return null
    }
}