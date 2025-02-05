package com.fengsheng.skill

import com.fengsheng.*
import com.fengsheng.phase.FightPhaseIdle
import com.fengsheng.protos.Role.skill_yi_hua_jie_mu_b_tos
import com.fengsheng.protos.skillYiHuaJieMuAToc
import com.fengsheng.protos.skillYiHuaJieMuATos
import com.fengsheng.protos.skillYiHuaJieMuBToc
import com.fengsheng.protos.skillYiHuaJieMuBTos
import com.google.protobuf.GeneratedMessage
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.TimeUnit

/**
 * 韩梅技能【移花接木】：争夺阶段，你可以翻开此角色牌，然后从一名角色的情报区选择一张情报，将其置入另一名角色的情报区，若如此做会让其收集三张或更多同色情报，则改为将该情报加入你的手牌。
 */
class YiHuaJieMu : ActiveSkill {
    override val skillId = SkillId.YI_HUA_JIE_MU

    override val isInitialSkill = true

    override fun canUse(fightPhase: FightPhaseIdle, r: Player): Boolean = !r.roleFaceUp

    override fun executeProtocol(g: Game, r: Player, message: GeneratedMessage) {
        val fsm = g.fsm as? FightPhaseIdle
        if (fsm == null || r !== fsm.whoseFightTurn) {
            logger.error("[移花接木]的使用时机不对")
            r.sendErrorMessage("[移花接木]的使用时机不对")
            return
        }
        if (r.roleFaceUp) {
            logger.error("你现在正面朝上，不能发动[移花接木]")
            r.sendErrorMessage("你现在正面朝上，不能发动[移花接木]")
            return
        }
        if (g.players.all { !it!!.alive || it.messageCards.isEmpty() }) {
            logger.error("场上没有情报，不能发动[移花接木]")
            r.sendErrorMessage("场上没有情报，不能发动[移花接木]")
            return
        }
        if (g.players.count { it!!.alive } < 2) {
            logger.error("场上没有两名存活的角色，不能发动[移花接木]")
            r.sendErrorMessage("场上没有两名存活的角色，不能发动[移花接木]")
            return
        }
        r.incrSeq()
        g.playerSetRoleFaceUp(r, true)
        r.addSkillUseCount(skillId)
        logger.info("${r}发动了[移花接木]")
        g.resolve(executeYiHuaJieMu(fsm, r))
    }

    private data class executeYiHuaJieMu(val fsm: FightPhaseIdle, val r: Player) : WaitingFsm {
        override fun resolve(): ResolveResult? {
            val alivePlayers = r.game!!.players.filterNotNull().filter { it.alive }
            val fromPlayer = alivePlayers.filter { it.messageCards.isNotEmpty() }.random()
            val card = fromPlayer.messageCards.random()
            val toPlayer = alivePlayers.filter { it !== fromPlayer }.random()
            r.game!!.players.send { p ->
                skillYiHuaJieMuAToc {
                    playerId = p.getAlternativeLocation(r.location)
                    waitingSecond = Config.WaitSecond * 2
                    if (p === r) {
                        val seq = p.seq
                        this.seq = seq
                        p.timeout = GameExecutor.post(p.game!!, {
                            if (p.checkSeq(seq)) {
                                r.game!!.tryContinueResolveProtocol(p, skillYiHuaJieMuBTos {
                                    fromPlayerId = p.getAlternativeLocation(fromPlayer.location)
                                    cardId = card.id
                                    toPlayerId = p.getAlternativeLocation(toPlayer.location)
                                    this.seq = seq
                                })
                            }
                        }, p.getWaitSeconds(waitingSecond + 2).toLong(), TimeUnit.SECONDS)
                    }
                }
            }
            if (r is RobotPlayer) {
                GameExecutor.post(r.game!!, {
                    r.game!!.tryContinueResolveProtocol(r, skillYiHuaJieMuBTos {
                        fromPlayerId = r.getAlternativeLocation(fromPlayer.location)
                        cardId = card.id
                        toPlayerId = r.getAlternativeLocation(toPlayer.location)
                    })
                }, 3, TimeUnit.SECONDS)
            }
            return null
        }

        override fun resolveProtocol(player: Player, message: GeneratedMessage): ResolveResult? {
            if (r !== player) {
                logger.error("不是你发技能的时机")
                player.sendErrorMessage("不是你发技能的时机")
                return null
            }
            if (message !is skill_yi_hua_jie_mu_b_tos) {
                logger.error("错误的协议")
                player.sendErrorMessage("错误的协议")
                return null
            }
            if (player is HumanPlayer && !player.checkSeq(message.seq)) {
                logger.error("操作太晚了, required Seq: ${player.seq}, actual Seq: ${message.seq}")
                player.sendErrorMessage("操作太晚了")
                return null
            }
            if (message.fromPlayerId < 0 || message.fromPlayerId >= player.game!!.players.size) {
                logger.error("目标错误")
                player.sendErrorMessage("目标错误")
                return null
            }
            val fromPlayer = player.game!!.players[player.getAbstractLocation(message.fromPlayerId)]!!
            if (!fromPlayer.alive) {
                logger.error("目标已死亡")
                player.sendErrorMessage("目标已死亡")
                return null
            }
            if (message.toPlayerId < 0 || message.toPlayerId >= player.game!!.players.size) {
                logger.error("目标错误")
                player.sendErrorMessage("目标错误")
                return null
            }
            val toPlayer = player.game!!.players[player.getAbstractLocation(message.toPlayerId)]!!
            if (!toPlayer.alive) {
                logger.error("目标已死亡")
                player.sendErrorMessage("目标已死亡")
                return null
            }
            if (message.fromPlayerId == message.toPlayerId) {
                logger.error("选择的两个目标不能相同")
                player.sendErrorMessage("选择的两个目标不能相同")
                return null
            }
            val card = fromPlayer.findMessageCard(message.cardId)
            if (card == null) {
                logger.error("没有这张卡")
                player.sendErrorMessage("没有这张卡")
                return null
            }
            player.incrSeq()
            fromPlayer.deleteMessageCard(card.id)
            val joinIntoHand = toPlayer.checkThreeSameMessageCard(card)
            if (joinIntoHand) {
                player.cards.add(card)
                logger.info("${fromPlayer}面前的${card}加入了${player}的手牌")
            } else {
                toPlayer.messageCards.add(card)
                player.game!!.addEvent(AddMessageCardEvent(fsm.whoseTurn))
                logger.info("${fromPlayer}面前的${card}加入了${toPlayer}的情报区")
            }
            player.game!!.players.send {
                skillYiHuaJieMuBToc {
                    cardId = card.id
                    this.joinIntoHand = joinIntoHand
                    playerId = it.getAlternativeLocation(player.location)
                    fromPlayerId = it.getAlternativeLocation(fromPlayer.location)
                    toPlayerId = it.getAlternativeLocation(toPlayer.location)
                }
            }
            return ResolveResult(fsm.copy(whoseFightTurn = fsm.inFrontOfWhom), true)
        }
    }

    companion object {
        fun ai(e: FightPhaseIdle, skill: ActiveSkill): Boolean {
            val p = e.whoseFightTurn
            if (p.roleFaceUp) return false
            val g = p.game!!
            if (g.players.all { !it!!.alive || it.messageCards.isEmpty() }) return false
            if (g.players.count { it!!.alive } < 2) return false
            GameExecutor.post(e.whoseFightTurn.game!!, {
                skill.executeProtocol(g, e.whoseFightTurn, skillYiHuaJieMuATos { })
            }, 3, TimeUnit.SECONDS)
            return true
        }
    }
}