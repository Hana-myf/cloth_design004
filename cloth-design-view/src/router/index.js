import Vue from "vue";
import VueRouter from "vue-router";
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import { getToken } from "@/utils/storage.js";
import echarts from 'echarts';
Vue.prototype.$echarts = echarts;
Vue.use(ElementUI);
Vue.use(VueRouter);

const routes = [
  {
    path: "*",
    redirect: "/login"
  },
  {
    path: "/login",
    component: () => import(`@/views/login/Login.vue`)
  },
  {
    path: "/register",
    component: () => import(`@/views/register/Register.vue`)
  },
  {
    path: "/message",
    component: () => import(`@/views/user/Message.vue`)
  },
  {
    path: "/record",
    component: () => import(`@/views/user/Record.vue`)
  },
  {
    path: "/admin",
    component: () => import(`@/views/admin/Home.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        path: "/adminLayout",
        name: '数据总览',
        icon: 'el-icon-data-analysis',
        component: () => import(`@/views/admin/Main.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/userManage",
        name: '用户管理',
        icon: 'el-icon-user-solid',
        component: () => import(`@/views/admin/UserManage.vue`),
        meta: { requireAuth: true },
      },

       {
        path: "/tagsManage",
        name: "资讯分类",
        icon: "el-icon-house",
        component: () => import(`@/views/admin/TagsManage.vue`),
        meta: { requireAuth: true }
      },
      {
        path: "/newsManage",
        name: "资讯管理",
        icon: "el-icon-document",
        component: () => import(`@/views/admin/NewsManage.vue`),
        meta: { requireAuth: true }
      },
       {
        path: "/messageManage",
        name: "消息管理",
        icon: "el-icon-message",
        component: () => import(`@/views/admin/MessageManage.vue`),
        meta: { requireAuth: true }
      },
      {
        path: "/evaluationsManage",
        name: '评论管理',
        icon: 'el-icon-chat-dot-round',
        component: () => import(`@/views/admin/EvaluationsManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/fabricManage",
        name: '面料管理',
        icon: 'el-icon-picture-outline',
        component: () => import(`@/views/admin/FabricManage.vue`),
        meta: { requireAuth: true },
      }
    ]
  },
  {
    path: "/user",
    component: () => import(`@/views/user/Main.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        name: '行业资讯',
        path: "/news-record",
        component: () => import(`@/views/user/Home.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-document', // 行业资讯 - 文档/文章
        },
      },
      {
        name: '收藏与创作',
        path: "/my-save",
        component: () => import(`@/views/user/NewsSave.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-star-off', // 收藏与创作 - 收藏/星星
        },
      },
      {
        name: '服装设计',
        path: "/user-health-model",
        component: () => import(`@/views/user/UserHealthModel.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-magic-stick', // 服装设计 - 魔法棒/设计
        },
      },
      {
        name: '行业资讯详情',
        path: "/news-detail",
        component: () => import(`@/views/user/NewsDetail.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-document-copy', // 行业资讯详情 - 文档复制/详情
        },
        isHidden: true,
      },
      {
        name: '搜索页',
        path: "/search-detail",
        component: () => import(`@/views/user/Search.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-search', // 搜索页 - 搜索
        },
        isHidden: true,
      },
      {
        name: "消息",
        path: "/message",
        component: () => import(`@/views/user/Message.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-message', // 消息 - 消息气泡
        },
        isHidden: true
      },
      {
        name: '细节优化',
        path: "/detail-optimization",
        component: () => import(`@/views/user/DetailOptimization.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-brush', // 细节优化 - 画笔/精修
        },
      },
      {
        name: '专业设计',
        // 注意：这里原来的 'icon' 属性就已经是直接在路由对象下，我将其移动到了 meta 内部
        path: "/pro-design",
        alias: "/user/pro-design",
        component: () => import(`@/views/user/ProfessionalDesign.vue`),
        meta: {
          requireAuth: true,
          icon: 'el-icon-edit', // 专业设计 - 编辑/铅笔
        },
      },
      // *** 在这里添加新的智能换装路由 ***
      {
        name: '智能换装', // 与菜单显示名称一致
        path: "/dressup", // 完整路径将是 /user/dressup
        component: () => import(`@/views/user/DressUp.vue`), // 你的组件路径
        meta: {
          requireAuth: true, // 确保需要认证才能访问
          icon: 'el-icon-scissors', // 智能换装 - 剪刀/时尚
        },
      },
    ]
  }
];
const router = new VueRouter({
  routes,
  // mode: 'history'
});
router.beforeEach((to, from, next) => {
  if (to.meta.requireAuth) {
    const token = getToken();
    if (token !== null) {
      next();
    } else {
      next("/login");
    }
  }
  else {
    next();
  }
});
export default router;
