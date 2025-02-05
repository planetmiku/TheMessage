package com.fengsheng.handler

import com.fengsheng.GameExecutor
import com.fengsheng.HumanPlayer
import com.google.protobuf.GeneratedMessage
import org.apache.logging.log4j.kotlin.logger

abstract class AbstractProtoHandler<T : GeneratedMessage> : ProtoHandler {
    protected abstract fun handle0(r: HumanPlayer, pb: T)
    override fun handle(player: HumanPlayer, message: GeneratedMessage) {
        // 因为player.setGame()只会join_room_tos调用，所以一定和这里的player.getGame()在同一线程，所以无需加锁
        val game = player.game
        if (game == null) {
            logger.error("player didn't join room, current msg: " + message.descriptorForType.name)
            player.sendErrorMessage("找不到房间")
        } else {
            GameExecutor.post(game) {
                player.clearTimeoutCount()
                @Suppress("UNCHECKED_CAST")
                handle0(player, message as T)
            }
        }
    }
}