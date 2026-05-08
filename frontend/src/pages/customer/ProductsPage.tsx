import { Card, Col, Input, Pagination, Row, Spin, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { http } from '@/lib/http'
import type { Product, SpringPage } from '@/types/api'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'

const { Meta } = Card
const pageSize = 12

export default function ProductsPage() {
  const [raw, setRaw] = useState<SpringPage<Product> | null>(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(1)
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebouncedValue(search, 300)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      try {
        const { data } = await http.get<SpringPage<Product>>('/api/products', {
          params: {
            page: page - 1,
            size: pageSize,
            sort: 'id,desc',
          },
        })
        if (!cancelled) setRaw(data)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [page])

  const filtered = useMemo(() => {
    const list = raw?.content ?? []
    const q = debouncedSearch.trim().toLowerCase()
    if (!q) return list
    return list.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        (p.description && p.description.toLowerCase().includes(q)),
    )
  }, [raw, debouncedSearch])

  return (
    <div>
      <Typography.Title level={4}>商品列表</Typography.Title>
      <Input.Search
        placeholder="搜索当前页商品名称 / 描述（防抖）"
        allowClear
        style={{ maxWidth: 360, marginBottom: 16 }}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          {filtered.map((p, idx) => (
            <Col xs={24} sm={12} md={8} lg={6} key={p.id}>
              <Card
                hoverable
                cover={
                  p.imageUrl ? (
                    <img
                      alt={p.name}
                      src={p.imageUrl}
                      loading={idx === 0 ? 'eager' : 'lazy'}
                      decoding="async"
                      style={{ height: 160, objectFit: 'cover', width: '100%' }}
                    />
                  ) : (
                    <div
                      style={{
                        height: 160,
                        background: '#f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#999',
                      }}
                    >
                      无图
                    </div>
                  )
                }
              >
                <Meta title={p.name} description={`¥${p.price} · 库存 ${p.stock}`} />
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>
      {raw ? (
        <div style={{ marginTop: 24, textAlign: 'center' }}>
          <Pagination
            current={page}
            pageSize={pageSize}
            total={raw.totalElements}
            onChange={setPage}
            showSizeChanger={false}
          />
        </div>
      ) : null}
    </div>
  )
}
