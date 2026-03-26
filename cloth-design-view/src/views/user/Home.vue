<template>
  <div class="news-container" @mousemove="handleMouseMove">
    <!-- 毛线光标（和登录页统一） -->
    <div class="yarn-cursor" :style="{ left: mouseX + 'px', top: mouseY + 'px' }">
      <div class="yarn-ball">🧶</div>
      <svg class="thread-tail">
        <path :d="threadPath" stroke="#e2b86e" stroke-width="1.5" fill="none" opacity="0.6" stroke-dasharray="4,3"/>
      </svg>
    </div>

    <!-- 顶部 Banner + 推荐资讯 -->
    <div class="news-top-wrapper">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="banner-card">
            <Banner :data="newsTopList" @on-click="onBannerClick" />
          </div>
        </el-col>

        <el-col :span="18">
          <el-row :gutter="20">
            <el-col
              @click.native="newsItemClick(news)"
              :span="6"
              :key="index"
              v-for="(news, index) in newsTopList"
            >
              <div class="top-news-card">
                <div class="card-image">
                  <img :src="news.cover" :alt="news.name" />
                  <div class="image-overlay"></div>
                </div>
                <div class="card-content">
                  <h2 class="card-title">{{ news.name }}</h2>
                  <div class="card-info">
                    <span class="news-tag">{{ news.tagName }}</span>
                    <span class="time">{{ parseTime(news.createTime) }}</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </div>

    <!-- 标签栏 -->
    <div class="tag-wrapper">
      <TagLine :dataList="tagsList" @on-click="tagOnClick" />
    </div>

    <!-- 资讯列表 -->
    <div class="news-list-wrapper">
      <div v-if="newsList.length === 0" class="empty-box">
        <el-empty description="暂无资讯"></el-empty>
      </div>

      <el-row :gutter="20">
        <el-col
          class="news-item-col"
          @click.native="newsItemClick(news)"
          :span="4"
          :key="index"
          v-for="(news, index) in newsList"
        >
          <div class="news-card">
            <div class="news-image">
              <img :src="news.cover" :alt="news.name" />
            </div>
            <div class="news-content">
              <h3 class="news-title">{{ news.name }}</h3>
              <div class="news-bottom">
                <span class="news-tag">{{ news.tagName }}</span>
                <span class="time">{{ parseTime(news.createTime) }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div class="pagination-box">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[12, 24, 36]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        ></el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
import TagLine from "@/components/TagLine"
import Banner from "@/components/Banner"
import { timeAgo } from "@/utils/data"

export default {
  components: { TagLine, Banner },
  data() {
    return {
      tagsList: [],
      newsList: [],
      newsTopList: [],
      newQueryDto: { tagId: null },
      currentPage: 1,
      pageSize: 12,
      total: 0,
      // 毛线光标
      mouseX: 0,
      mouseY: 0,
      history: []
    }
  },
  created() {
    this.loadAllTags()
    this.loadAllNews()
    this.loadAllTopNews()
  },
  computed: {
    threadPath() {
      if (this.history.length < 2) return ''
      return `M ${this.history.map(p => `${p.x - this.mouseX},${p.y - this.mouseY}`).join(' L ')}`
    }
  },
  methods: {
    // 毛线光标
    handleMouseMove(e) {
      this.mouseX = e.clientX
      this.mouseY = e.clientY
      this.history.push({ x: e.clientX, y: e.clientY })
      if (this.history.length > 12) this.history.shift()
    },
    onBannerClick(banner) {
      sessionStorage.setItem('newsInfo', JSON.stringify(banner))
      this.$router.push('/news-detail')
    },
    newsItemClick(news) {
      sessionStorage.setItem('newsInfo', JSON.stringify(news))
      this.$router.push('/news-detail')
    },
    parseTime(time) {
      return timeAgo(time)
    },
    tagOnClick(tags) {
      this.newQueryDto.tagId = tags.id
      this.currentPage = 1
      this.loadAllNews()
    },
    loadAllTags() {
      this.$axios.post('/tags/query', {}).then(res => {
        if (res.data.code === 200) {
          this.tagsList = res.data.data
          this.tagsList.unshift({ name: '全部', id: null })
        }
      })
    },
    loadAllTopNews() {
      this.$axios.get(`/news/queryRecommend/${4}`).then(res => {
        if (res.data.code === 200) {
          this.newsTopList = res.data.data
        }
      })
    },
    loadAllNews() {
      this.$axios.post('/news/query', this.newQueryDto).then(res => {
        if (res.data.code === 200) {
          const allNews = res.data.data
          this.total = allNews.length
          const start = (this.currentPage - 1) * this.pageSize
          const end = start + this.pageSize
          this.newsList = allNews.slice(start, end)
        }
      })
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.loadAllNews()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.loadAllNews()
    }
  }
}
</script>

<style lang="scss" scoped>
// 全局容器（统一登录页风格）
.news-container {
  width: 100%;
  min-height: 100vh;
  background: #fdfaf5;
  padding: 30px 40px;
  box-sizing: border-box;
  font-family: "PingFang SC", "Optima", sans-serif;
  cursor: none;
}

// 毛线光标（和登录页完全一致）
.yarn-cursor {
  position: fixed;
  pointer-events: none;
  z-index: 9999;
  transform: translate(-50%, -50%);
  .yarn-ball {
    font-size: 32px;
    filter: drop-shadow(0 5px 10px rgba(0,0,0,0.1));
  }
  .thread-tail {
    position: absolute;
    top: 0;
    left: 0;
    overflow: visible;
    width: 1px;
    height: 1px;
  }
}

// 顶部区域
.news-top-wrapper {
  margin-bottom: 30px;
}

// Banner 卡片
.banner-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(74, 58, 42, 0.08);
}

// 推荐资讯卡片
.top-news-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(74, 58, 42, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(74, 58, 42, 0.12);
  }

  .card-image {
    position: relative;
    width: 100%;
    height: 160px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: 0.4s;
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: linear-gradient(to top, rgba(0,0,0,0.2), transparent);
    }
  }

  .card-content {
    padding: 14px 16px;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #3d2f24;
      margin: 0 0 8px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .card-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
    }
  }
}

// 标签栏
.tag-wrapper {
  margin-bottom: 30px;
}

// 资讯列表容器
.news-list-wrapper {
  background: #fff;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(74, 58, 42, 0.06);
}

// 空状态
.empty-box {
  padding: 60px 0;
  text-align: center;
}

// 资讯卡片列
.news-item-col {
  margin-bottom: 20px;
}

// 资讯卡片
.news-card {
  background: #fdfaf5;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid rgba(226, 184, 110, 0.12);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(74, 58, 42, 0.1);
    border-color: rgba(226, 184, 110, 0.3);
  }

  .news-image {
    width: 100%;
    height: 130px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: 0.4s;
    }
  }

  .news-content {
    padding: 14px;

    .news-title {
      font-size: 15px;
      font-weight: 600;
      color: #3d2f24;
      margin: 0 0 10px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .news-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
    }
  }
}

// ✅ 统一标签样式（修改后：缩小字号 + 禁止换行）
.news-tag {
  display: inline-block;
  padding: 2px 6px;
  background: #e2b86e;
  color: #fff;
  border-radius: 4px;
  font-weight: 500;
  font-size: 11px; /* 从12px缩小到11px */
  white-space: nowrap; /* 强制不换行 */
}

// 时间样式
.time {
  color: #bca080;
}

// 分页
.pagination-box {
  margin-top: 30px;
  text-align: center;

  :deep(.el-pagination) {
    color: #3d2f24;

    button {
      border-color: rgba(226, 184, 110, 0.3);
      color: #3d2f24;

      &:hover {
        color: #e2b86e;
        border-color: #e2b86e;
        background: transparent;
      }
    }

    .el-pager li.active {
      background: #e2b86e;
      color: #fff;
      font-weight: bold;
    }
  }
}
</style>