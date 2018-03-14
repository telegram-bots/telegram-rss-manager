package com.github.telegram_bots.core.domain

import com.github.telegram_bots.core.domain.Types.{ChannelURL, PostID}

case class Channel(id: Int, url: ChannelURL, lastPostId: PostID)