package com.fengsheng.handler

import com.fengsheng.HumanPlayer
import com.fengsheng.protos.Common.card_type
import com.fengsheng.protos.Fengsheng
import com.fengsheng.skill.RuBiZhiShi.excuteRuBiZhiShi
import org.apache.log4j.Logger

class use_cheng_qing_tos : AbstractProtoHandler<Fengsheng.use_cheng_qing_tos>() {
    override fun handle0(r: HumanPlayer, pb: Fengsheng.use_cheng_qing_tos) {
        if (!r.checkSeq(pb.seq)) {
            log.error("操作太晚了, required Seq: ${r.seq}, actual Seq: ${pb.seq}")
            r.sendErrorMessage("操作太晚了")
            return
        }
        if (r.game!!.fsm is excuteRuBiZhiShi) {
            r.game!!.tryContinueResolveProtocol(r, pb)
            return
        }
        val card = r.findCard(pb.cardId)
        if (card == null) {
            log.error("没有这张牌")
            r.sendErrorMessage("没有这张牌")
            return
        }
        if (card.type != card_type.Cheng_Qing) {
            log.error("这张牌不是澄清，而是$card")
            r.sendErrorMessage("这张牌不是澄清，而是$card")
            return
        }
        if (pb.playerId < 0 || pb.playerId >= r.game!!.players.size) {
            log.error("目标错误: ${pb.playerId}")
            r.sendErrorMessage("目标错误: ${pb.playerId}")
            return
        }
        val target = r.game!!.players[r.getAbstractLocation(pb.playerId)]!!
        if (card.canUse(r.game!!, r, target, pb.targetCardId)) {
            r.incrSeq()
            card.execute(r.game!!, r, target, pb.targetCardId)
        }
    }

    companion object {
        private val log = Logger.getLogger(use_cheng_qing_tos::class.java)
    }
}