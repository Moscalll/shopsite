import { useEffect } from 'react'
import { RouterProvider } from 'react-router-dom'
import { ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { router } from '@/app/router'
import { useAuthStore } from '@/stores/authStore'

export default function App() {
  const hydrateFromStorage = useAuthStore((s) => s.hydrateFromStorage)
  const hydrated = useAuthStore((s) => s.hydrated)

  useEffect(() => {
    void hydrateFromStorage()
  }, [hydrateFromStorage])

  if (!hydrated) {
    return <Spin size="large" style={{ display: 'block', margin: '40vh auto' }} />
  }

  return (
    <ConfigProvider locale={zhCN}>
      <RouterProvider router={router} />
    </ConfigProvider>
  )
}
