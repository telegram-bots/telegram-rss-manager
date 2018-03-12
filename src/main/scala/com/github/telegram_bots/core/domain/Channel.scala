package com.github.telegram_bots.core.domain

import com.github.telegram_bots.core.domain.Types.{ChannelURL, ID, PostID}

case class Channel(id: ID, url: ChannelURL, lastPostId: PostID)