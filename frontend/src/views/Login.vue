<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>BGAI 系统登录</h1>
        <p class="subtitle">请使用微信扫码登录</p>
      </div>
      <WechatLogin />
    </div>
  </div>
</template>

<script setup>
import WechatLogin from '@/components/WechatLogin.vue'
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-container {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 480px;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-header h1 {
  color: #333;
  margin: 0;
  font-size: 1.8rem;
}

.subtitle {
  color: #666;
  margin: 0.5rem 0 0;
  font-size: 1rem;
}
</style> 