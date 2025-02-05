package com.fengsheng.phase

import com.fengsheng.Fsm
import com.fengsheng.ResolveResult
import com.fengsheng.protos.Common.direction.Left
import com.fengsheng.protos.Common.direction.Up
import com.fengsheng.protos.Common.phase.Send_Phase
import com.fengsheng.protos.notifyPhaseToc
import com.fengsheng.send
import org.apache.logging.log4j.kotlin.logger

/**
 * 情报传递阶段，情报移到下一个人
 *
 * @param sendPhase 原先那个人的 [SendPhaseIdle] （不是下一个人的）
 */
data class MessageMoveNext(val sendPhase: SendPhaseIdle) : Fsm {
    override fun resolve(): ResolveResult {
        if (sendPhase.dir == Up) {
            return if (sendPhase.sender.alive) {
                logger.info("情报到达${sendPhase.sender}面前")
                ResolveResult(sendPhase.copy(inFrontOfWhom = sendPhase.sender), true)
            } else {
                nextTurn()
            }
        } else {
            val players = sendPhase.whoseTurn.game!!.players
            var inFrontOfWhom = sendPhase.inFrontOfWhom.location
            while (true) {
                inFrontOfWhom =
                    if (sendPhase.dir == Left) (inFrontOfWhom + players.size - 1) % players.size
                    else (inFrontOfWhom + 1) % players.size
                if (players[inFrontOfWhom]!!.alive) {
                    logger.info("情报到达${players[inFrontOfWhom]}面前")
                    return ResolveResult(sendPhase.copy(inFrontOfWhom = players[inFrontOfWhom]!!), true)
                } else if (sendPhase.sender === players[inFrontOfWhom]) {
                    return nextTurn()
                }
            }
        }
    }

    private fun nextTurn(): ResolveResult {
        sendPhase.inFrontOfWhom.game!!.deck.discard(sendPhase.messageCard)
        if (!sendPhase.isMessageCardFaceUp) {
            val players = sendPhase.whoseTurn.game!!.players
            players.send {
                notifyPhaseToc {
                    currentPlayerId = it.getAlternativeLocation(sendPhase.whoseTurn.location)
                    currentPhase = Send_Phase
                    messagePlayerId = it.getAlternativeLocation(sendPhase.inFrontOfWhom.location)
                    messageCardDir = sendPhase.dir
                    messageCard = sendPhase.messageCard.toPbCard()
                    senderId = it.getAlternativeLocation(sendPhase.sender.location)
                    waitingPlayerId = it.getAlternativeLocation(sendPhase.inFrontOfWhom.location)
                }
            }
        }
        return ResolveResult(NextTurn(sendPhase.whoseTurn), true)
    }
}