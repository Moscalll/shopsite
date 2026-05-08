import { Layout, Menu, Button } from 'antd'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const { Header, Content } = Layout

export default function MerchantLayout() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <span style={{ color: '#fff', fontWeight: 600 }}>商家后台</span>
        <Menu
          theme="dark"
          mode="horizontal"
          selectable={false}
          style={{ flex: 1 }}
          items={[
            { key: 'p', label: <Link to="/merchant">商品管理</Link> },
            { key: 'shop', label: <Link to="/">回商城</Link> },
          ]}
        />
        <span style={{ color: '#fff', opacity: 0.85 }}>{user?.username}</span>
        <Button
          type="link"
          style={{ color: '#fff' }}
          onClick={() => {
            logout()
            navigate('/login')
          }}
        >
          退出
        </Button>
      </Header>
      <Content style={{ padding: 24 }}>
        <Outlet />
      </Content>
    </Layout>
  )
}
