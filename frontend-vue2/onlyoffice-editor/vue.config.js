const { defineConfig } = require('@vue/cli-service');

module.exports = defineConfig({
  transpileDependencies: true,
  
  // 开发服务器配置
  devServer: {
    port: 8081,
    // 允许跨域
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
  
  // 生产环境配置
  productionSourceMap: false,
  
  // 输出目录
  outputDir: 'dist',
  
  // 静态资源目录
  assetsDir: 'static',
});
