import { Button, Modal, Space, Tag, Typography, message } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { PaginatedTable } from '@/components/business/PaginatedTable'
import { ProductModalForm } from '@/components/business/ProductModalForm'
import { http } from '@/lib/http'
import type { Category, Product, SpringPage } from '@/types/api'

export default function MerchantProductsPage() {
  const [data, setData] = useState<SpringPage<Product> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const pageSize = 10
  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create')
  const [editing, setEditing] = useState<Product | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const { data: pageData } = await http.get<SpringPage<Product>>('/api/products/mine', {
        params: { page: page - 1, size: pageSize, sort: 'id,desc' },
      })
      setData(pageData)
    } catch {
      message.error('加载商品失败')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    ;(async () => {
      try {
        const { data } = await http.get<Category[]>('/api/categories')
        setCategories(data)
      } catch {
        message.error('加载分类失败')
      }
    })()
  }, [])

  const onDelete = (record: Product) => {
    Modal.confirm({
      title: '删除商品？',
      onOk: async () => {
        await http.delete(`/api/products/${record.id}`)
        message.success('已删除或已下架')
        await load()
      },
    })
  }

  return (
    <div>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          我的商品
        </Typography.Title>
        <Button
          type="primary"
          onClick={() => {
            setModalMode('create')
            setEditing(null)
            setModalOpen(true)
          }}
        >
          新建商品
        </Button>
      </Space>

      <PaginatedTable<Product>
        rowKey="id"
        loading={loading}
        dataSource={data?.content ?? []}
        pagination={{
          current: page,
          pageSize,
          total: data?.totalElements ?? 0,
          showSizeChanger: false,
          onChange: (p) => setPage(p),
        }}
        columns={[
          { title: '名称', dataIndex: 'name', key: 'name' },
          { title: '价格', dataIndex: 'price', key: 'price', render: (v: number) => `¥${v}` },
          { title: '库存', dataIndex: 'stock', key: 'stock' },
          {
            title: '上架',
            dataIndex: 'isAvailable',
            key: 'isAvailable',
            render: (v: boolean) => (v ? <Tag color="green">是</Tag> : <Tag>否</Tag>),
          },
          {
            title: '操作',
            key: 'actions',
            render: (_, record) => (
              <Space>
                <Button
                  type="link"
                  onClick={() => {
                    setModalMode('edit')
                    setEditing(record)
                    setModalOpen(true)
                  }}
                >
                  编辑
                </Button>
                <Button type="link" danger onClick={() => onDelete(record)}>
                  删除
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <ProductModalForm
        open={modalOpen}
        mode={modalMode}
        categories={categories}
        initial={editing}
        onClose={() => setModalOpen(false)}
        onSuccess={() => load()}
      />
    </div>
  )
}
