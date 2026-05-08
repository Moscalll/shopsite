import { Button, Card, Form, Input, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import axios from 'axios'
import type { ApiMessageResponse } from '@/types/api'

export default function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const onFinish = async (v: { username: string; password: string }) => {
    try {
      await login(v.username, v.password)
      message.success('登录成功')
      const role = useAuthStore.getState().user?.role
      if (role === 'MERCHANT' || role === 'ADMIN') {
        navigate('/merchant', { replace: true })
      } else {
        navigate('/', { replace: true })
      }
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
        const m = e.response.data as ApiMessageResponse
        message.error(m.message ?? '登录失败')
      } else {
        message.error('登录失败')
      }
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '10vh auto', padding: 16 }}>
      <Typography.Title level={3} style={{ textAlign: 'center' }}>
        ShopSite 登录
      </Typography.Title>
      <Card>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            登录（JWT）
          </Button>
        </Form>
      </Card>
    </div>
  )
}
