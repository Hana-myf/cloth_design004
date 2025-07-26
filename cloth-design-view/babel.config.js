module.exports = {
 presets: [
    // Vue 项目默认预设，已包含对 ES6+ 语法的转换
    '@vue/cli-plugin-babel/preset'
  ],
  // 新增插件支持可选链等新语法
  plugins: [
    '@babel/plugin-proposal-optional-chaining'
  ]
}
