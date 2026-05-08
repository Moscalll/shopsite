import { Layout, Menu, Button } from 'antd'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const { Header, Content } = Layout

export default function CustomerLayout() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  const merchantEntry =
    user && (user.role === 'MERCHANT' || user.role === 'ADMIN') ? (
      <Link to="/merchant">商家后台</Link>
    ) : null

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Link to="/" style={{ color: '#fff', fontWeight: 600 }}>
          ShopSite
        </Link>
        <Menu
          theme="dark"
          mode="horizontal"
          selectable={false}
          style={{ flex: 1, minWidth: 0 }}
          items={[
            { key: 'home', label: <Link to="/">商城</Link> },
            ...(merchantEntry ? [{ key: 'm', label: merchantEntry }] : []),
          ]}
        />
        <span style={{ color: '#fff', opacity: 0.85 }}>
          {user ? user.username : '未登录'}
        </span>
        {user ? (
          <Button type="link" style={{ color: '#fff' }} onClick={() => { logout(); navigate('/login') }}>
            退出
          </Button>
        ) : (
          <Link to="/login" style={{ color: '#fff' }}>
            登录
          </Link>
        )}
      </Header>
      <Content style={{ padding: 24, maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>
    </Layout>
  )
}
