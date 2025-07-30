// src/store/index.js
import Vue from 'vue';
import Vuex from 'vuex';
import user from './modules/user'; // 确保 user.js 路径正确且无语法错误

// 关键：注册 Vuex（必须在创建 Store 之前）
Vue.use(Vuex);

// 创建 Store 实例
const store = new Vuex.Store({
  modules: {
    user
  }
});

export default store;