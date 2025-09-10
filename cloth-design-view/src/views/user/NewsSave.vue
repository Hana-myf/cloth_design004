<template>
  <div class="news-collection-page">
    <!-- 新闻收藏区域 -->
    <div class="section news-collection">
      <div class="section-header">
        <h2>我的收藏</h2>
        <div class="divider"></div>
      </div>
      
      <div v-if="newsSaveList.length === 0" class="empty-state">
        <el-empty description="暂未收藏资讯" />
      </div>
      
      <div class="news-grid" v-else>
        <el-col
          @click.native="newsItemClick(news)"
          :span="4"
          :key="index"
          v-for="(news, index) in newsSaveList"
          class="news-card"
        >
          <div class="card-inner">
            <div class="news-image">
              <img
                :src="news.cover || defaultNewsImg"
                :alt="news.name"
                @error="news.cover = defaultNewsImg"
              />
              <div class="image-overlay"></div>
            </div>
            <div class="news-content">
              <h3 class="news-title">{{ news.name }}</h3>
              <div class="news-meta">
                <span class="news-tags">{{ news.tagName }}</span>
                <span class="news-time">收藏于 {{ parseTime(news.createTime) }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </div>
    </div>
    
    <!-- 我的创作区域 -->
    <div class="section my-creations">
      <div class="section-header">
        <h2>我的创作</h2>
        <div class="divider"></div>
      </div>
      
      <ImageSaveList 
        :savedImages="savedImages" 
        class="creation-list"
      />
    </div>
  </div>
</template>

<script>
import ImageSaveList from '@/components/ImageSaveList.vue'; // 复用已有组件
import { timeAgo } from "@/utils/data";

export default {
  components: { ImageSaveList },
  data() {
    return {
      newsSaveList: [],
      savedImages: [], // 我的创作图片列表
      defaultNewsImg: require('@/assets/default-news.png'), // 默认新闻封面
    };
  },
  created() {
    this.loadAllSaveNews();
    this.loadUserImages(); // 加载我的创作
  },
  methods: {
    parseTime(time) {
      return timeAgo(time);
    },
    newsItemClick(newsSave) {
      const news = {
        id: newsSave.newsId,
        name: newsSave.name,
        content: newsSave.content,
        createTime: newsSave.createTime,
        tagName: newsSave.tagName
      };
      sessionStorage.setItem("newsInfo", JSON.stringify(news));
      this.$router.push("/news-detail");
    },
    loadAllSaveNews() {
      const userInfo = JSON.parse(sessionStorage.getItem("userInfo") || '{}');
      this.$axios.post("/news-save/query", {
        userId: userInfo.id
      }).then(response => {
        if (response.data.code === 200) {
          this.newsSaveList = response.data.data;
        }
      });
    },
    // 加载我的创作（复用CozeChat中的逻辑）
    loadUserImages() {
      const userInfo = JSON.parse(sessionStorage.getItem("userInfo") || '{}');
      if (!userInfo.id) return;
      
      this.$axios.post('/generatedImage/query', {
        userId: userInfo.id,
        pageNum: 1,
        pageSize: 4
      }).then(response => {
        if (response.data.code === 200) {
          this.savedImages = response.data.data.sort(
            (a, b) => new Date(b.generateTime) - new Date(a.generateTime)
          ).slice(0, 4);
        }
      });
    }
  }
};
</script>

<style scoped lang="scss">
.news-collection-page {
  padding: 30px;
  max-width: 1400px;
  margin: 0 auto;
  background-color: #fcfcfa;
  min-height: calc(100vh - 120px);
}

.section {
  margin-bottom: 60px;
  position: relative;
  
  &.news-collection {
    padding-top: 10px;
  }
  
  &.my-creations {
    padding-top: 20px;
  }
}

.section-header {
  margin-bottom: 30px;
  
  h2 {
    font-size: 24px;
    color: #2d2d2a;
    margin: 0 0 12px 0;
    font-weight: 500;
    letter-spacing: 0.5px;
  }
  
  .divider {
    height: 2px;
    width: 60px;
    background-color: #d4af37; /* 金色作为强调色，符合服装设计感 */
    border-radius: 1px;
    transition: width 0.3s ease;
    
    /* 悬停时的微妙效果 */
    .section-header:hover & {
      width: 80px;
    }
  }
}

.empty-state {
  padding: 80px 0;
  text-align: center;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.news-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 28px;
  margin-top: 20px;
}

.news-card {
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.08);
  }
  
  .card-inner {
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: box-shadow 0.3s ease;
  }
  
  .news-image {
    height: 180px;
    overflow: hidden;
    position: relative;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.6s ease;
    }
    
    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(0,0,0,0.1) 100%);
      z-index: 1;
      transition: background 0.3s ease;
    }
    
    &:hover img {
      transform: scale(1.08);
    }
    
    &:hover .image-overlay {
      background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(0,0,0,0.2) 100%);
    }
  }
  
  .news-content {
    padding: 18px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  
  .news-title {
    font-size: 16px;
    color: #2d2d2a;
    margin: 0 0 12px 0;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-weight: 500;
  }
  
  .news-meta {
    display: flex;
    align-items: center;
    margin-top: auto;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .news-tags {
    font-size: 12px;
    padding: 3px 8px;
    background-color: #f5f1e6;
    color: #8b7d5e;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  
  .news-time {
    font-size: 12px;
    color: #99958c;
    white-space: nowrap;
  }
}

.creation-list {
  margin-top: 10px;
  background-color: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* 为ImageSaveList组件添加样式适配 */
::v-deep .creation-list {
  .image-item {
    border-radius: 8px;
    overflow: hidden;
    transition: transform 0.3s ease;
    
    &:hover {
      transform: scale(1.03);
    }
  }
  
  .image-info {
    color: #2d2d2a;
    font-size: 14px;
  }
}
</style>