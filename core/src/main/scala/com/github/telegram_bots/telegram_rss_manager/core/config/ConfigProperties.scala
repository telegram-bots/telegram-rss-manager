package com.github.telegram_bots.telegram_rss_manager.core.config

import com.typesafe.config.Config

class ConfigProperties(root: Config, path: String) {
  protected val self: Config = root.getConfig(path)
}
