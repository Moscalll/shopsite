import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import CustomerLayout from '@/app/layouts/CustomerLayout'
import MerchantLayout from '@/app/layouts/MerchantLayout'
import { RequireRole } from '@/app/RequireRole'

const LoginPage = lazy(() => import('@/pages/LoginPage'))
const ProductsPage = lazy(() => import('@/pages/customer/ProductsPage'))
const MerchantProductsPage = lazy(() => import('@/pages/merchant/MerchantProductsPage'))

function SuspenseSpin({ children }: { children: React.ReactNode }) {
  return (
    <Suspense
      fallback={<Spin size="large" style={{ display: 'block', margin: '30vh auto' }} />}
    >
      {children}
    </Suspense>
  )
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <SuspenseSpin>
        <LoginPage />
      </SuspenseSpin>
    ),
  },
  {
    path: '/',
    element: <CustomerLayout />,
    children: [
      {
        index: true,
        element: (
          <SuspenseSpin>
            <ProductsPage />
          </SuspenseSpin>
        ),
      },
    ],
  },
  {
    path: '/merchant',
    element: (
      <RequireRole roles={['MERCHANT', 'ADMIN']}>
        <MerchantLayout />
      </RequireRole>
    ),
    children: [
      {
        index: true,
        element: (
          <SuspenseSpin>
            <MerchantProductsPage />
          </SuspenseSpin>
        ),
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
