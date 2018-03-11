package com.github.telegram_bots.core.domain

import com.github.telegram_bots.core.domain.types.{ChannelURL, PostID}

case class Channel(url: ChannelURL, lastPostId: PostID)