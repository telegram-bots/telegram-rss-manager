package com.github.telegram_bots.core.config

import com.typesafe.config.Config

class ConfigProperties(root: Config, path: String) {
  protected val self: Config = root.getConfig(path)
}
