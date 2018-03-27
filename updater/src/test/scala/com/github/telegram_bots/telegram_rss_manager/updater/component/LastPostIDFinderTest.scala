package com.github.telegram_bots.telegram_rss_manager.updater.component

import org.scalatest.FunSuite

class LastPostIDFinderTest extends FunSuite {
  def findLastPostId(constantLastPostId: Int): (Int, Int) = {
    val (lastPostId, path) = LastPostIDFinder.findLastPostID("channel1", (_, postId) => postId <= constantLastPostId)

    (lastPostId, path.size)
  }

 test("correctly determines lastPostId in least possible number of steps") {
   assert(findLastPostId(70) == (70, 12))
   assert(findLastPostId(507) == (507, 13))
   assert(findLastPostId(2633) == (2633, 13))
   assert(findLastPostId(20083) == (20083, 20))
   assert(findLastPostId(303941) == (303941, 27))
 }
}
