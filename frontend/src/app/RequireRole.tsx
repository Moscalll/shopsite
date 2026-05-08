import { Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuthStore } from '@/stores/authStore'

interface RequireRoleProps {
  roles: string[]
  children: React.ReactNode
}

export function RequireRole({ roles, children }: RequireRoleProps) {
  const hydrated = useAuthStore((s) => s.hydrated)
  const user = useAuthStore((s) => s.user)

  if (!hydrated) {
    return <Spin size="large" style={{ display: 'block', margin: '30vh auto' }} />
  }
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}
