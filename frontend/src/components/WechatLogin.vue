<template>
  <div class="login-container">
    <div v-if="!isLoggedIn" class="qrcode-section">
      <h2>微信扫码登录</h2>
      <div ref="qrcodeContainer" class="qrcode-wrapper"></div>
      <p v-if="loading" class="status-text">生成二维码中...</p>
      <p v-if="expired" class="status-text">
        二维码已过期，
        <button @click="refreshQR" class="refresh-btn">点击刷新</button>
      </p>
    </div>
    <div v-else class="user-info">
      <h2>登录成功</h2>
      <div class="user-profile">
        <img :src="userInfo.avatar" class="avatar" alt="用户头像" />
        <p class="nickname">{{ userInfo.nickname }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import QRCode from 'qrcodejs2'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const qrcodeContainer = ref(null)
const loading = ref(true)
const expired = ref(false)
const isLoggedIn = ref(false)
const userInfo = ref(null)

let qrcode = null
let pollTimer = null

// 初始化二维码
const initQRCode = async () => {
  try {
    const { qrUrl, tempToken } = await authStore.getWechatQRCode()
    if (qrcodeContainer.value) {
      qrcodeContainer.value.innerHTML = ''
      qrcode = new QRCode(qrcodeContainer.value, {
        text: qrUrl,
        width: 200,
        height: 200,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.H
      })
      startPolling(tempToken)
    }
  } catch (error) {
    console.error('二维码生成失败:', error)
  } finally {
    loading.value = false
  }
}

// 轮询登录状态
const startPolling = (tempToken) => {
  pollTimer = setInterval(async () => {
    try {
      const status = await authStore.checkLoginStatus(tempToken)
      if (status === 'authorized') {
        clearInterval(pollTimer)
        const userData = await authStore.fetchUserInfo()
        userInfo.value = userData
        isLoggedIn.value = true
      } else if (status === 'expired') {
        expired.value = true
        clearInterval(pollTimer)
      }
    } catch (error) {
      console.error('检查登录状态失败:', error)
    }
  }, 3000)
}

// 刷新二维码
const refreshQR = () => {
  clearInterval(pollTimer)
  expired.value = false
  loading.value = true
  initQRCode()
}

onMounted(initQRCode)
onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  max-width: 400px;
  margin: 0 auto;
}

.qrcode-section {
  text-align: center;
}

.qrcode-wrapper {
  margin: 1rem 0;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
  background: white;
}

.status-text {
  color: #666;
  margin: 1rem 0;
}

.refresh-btn {
  background: #07c160;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}

.refresh-btn:hover {
  background: #06ad56;
}

.user-info {
  text-align: center;
}

.user-profile {
  margin-top: 1rem;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 1rem;
}

.nickname {
  font-size: 1.2rem;
  color: #333;
  margin: 0;
}
</style> 