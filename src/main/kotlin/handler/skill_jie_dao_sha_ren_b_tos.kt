package com.fengsheng.handler

import com.fengsheng.HumanPlayer
import com.fengsheng.protos.Role
import com.fengsheng.skill.SkillId
import org.apache.logging.log4j.kotlin.logger

class skill_jie_dao_sha_ren_b_tos : AbstractProtoHandler<Role.skill_jie_dao_sha_ren_b_tos>() {
    override fun handle0(r: HumanPlayer, pb: Role.skill_jie_dao_sha_ren_b_tos) {
        val skill = r.findSkill(SkillId.JIE_DAO_SHA_REN)
        if (skill == null) {
            logger.error("你没有这个技能")
            r.sendErrorMessage("你没有这个技能")
            return
        }
        r.game!!.tryContinueResolveProtocol(r, pb)
    }
}