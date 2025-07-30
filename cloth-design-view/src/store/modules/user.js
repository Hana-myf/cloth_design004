// 存储用户信息、登录状态、用户ID等
const state = {
  userInfo: null, // 完整用户信息（包含id、角色等）
  token: ''       // 登录令牌
};

const mutations = {
  // 保存用户信息（登录时调用）
  SAVE_USER_INFO(state, data) {
    state.userInfo = data; // data需包含id、role等字段
    state.token = data.token;
  },
  // 清除用户信息（登出时调用）
  CLEAR_USER_INFO(state) {
    state.userInfo = null;
    state.token = '';
  }
};

const actions = {
  // 登录成功后保存用户信息
  login({ commit }, userData) {
    commit('SAVE_USER_INFO', userData);
  },
  // 登出
  logout({ commit }) {
    commit('CLEAR_USER_INFO');
  }
};

const getters = {
  // 获取用户ID
  userId: (state) => state.userInfo?.id || null,
  // 判断是否登录
  isLoggedIn: (state) => !!state.token,
  // 获取用户角色
  userRole: (state) => state.userInfo?.role || null
};

export default {
  namespaced: true, // 开启命名空间，避免模块间命名冲突
  state,
  mutations,
  actions,
  getters
};