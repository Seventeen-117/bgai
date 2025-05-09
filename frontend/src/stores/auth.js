import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const userInfo = ref(null)
  const tempToken = ref(null)

  const getWechatQRCode = async () => {
    try {
      const response = await axios.get('/auth/wechat/qrcode')
      tempToken.value = response.data.tempToken
      return response.data
    } catch (error) {
      console.error('获取二维码失败:', error)
      throw new Error('获取二维码失败')
    }
  }

  const checkLoginStatus = async (token) => {
    try {
      const response = await axios.post('/auth/wechat/check', { tempToken: token })
      if (response.data.status === 'authorized') {
        localStorage.setItem('access_token', response.data.token)
        isLoggedIn.value = true
      }
      return response.data.status
    } catch (error) {
      console.error('检查登录状态失败:', error)
      return 'error'
    }
  }

  const fetchUserInfo = async () => {
    try {
      const response = await axios.get('/user/info')
      userInfo.value = response.data
      return response.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  }

  const logout = () => {
    localStorage.removeItem('access_token')
    isLoggedIn.value = false
    userInfo.value = null
    tempToken.value = null
  }

  return {
    isLoggedIn,
    userInfo,
    tempToken,
    getWechatQRCode,
    checkLoginStatus,
    fetchUserInfo,
    logout
  }
}) 