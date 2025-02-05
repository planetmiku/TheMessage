package com.fengsheng.handler

import com.fengsheng.HumanPlayer
import com.fengsheng.protos.Common.card_type.Po_Yi
import com.fengsheng.protos.Fengsheng
import com.fengsheng.skill.canUseCardTypes
import org.apache.logging.log4j.kotlin.logger

class use_po_yi_tos : AbstractProtoHandler<Fengsheng.use_po_yi_tos>() {
    override fun handle0(r: HumanPlayer, pb: Fengsheng.use_po_yi_tos) {
        if (!r.checkSeq(pb.seq)) {
            logger.error("操作太晚了, required Seq: ${r.seq}, actual Seq: ${pb.seq}")
            r.sendErrorMessage("操作太晚了")
            return
        }
        var card = r.findCard(pb.cardId)
        if (card == null) {
            logger.error("没有这张牌")
            r.sendErrorMessage("没有这张牌")
            return
        }
        val (ok, convertCardSkill) = r.canUseCardTypes(Po_Yi, card)
        if (!ok) {
            logger.error("这张${card}不能当作破译使用")
            r.sendErrorMessage("这张${card}不能当作破译使用")
            return
        }
        if (card.type != Po_Yi) card = card.asCard(Po_Yi)
        if (card.canUse(r.game!!, r)) {
            r.incrSeq()
            convertCardSkill?.onConvert(r)
            card.execute(r.game!!, r)
        }
    }
}