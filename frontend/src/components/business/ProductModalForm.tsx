import { Form, Input, InputNumber, Modal, Select, Switch } from 'antd'
import { useEffect } from 'react'
import type { Category, Product } from '@/types/api'
import { http } from '@/lib/http'

type Mode = 'create' | 'edit'

interface ProductModalFormProps {
  open: boolean
  mode: Mode
  categories: Category[]
  initial?: Product | null
  onClose: () => void
  onSuccess: () => void
}

export function ProductModalForm({
  open,
  mode,
  categories,
  initial,
  onClose,
  onSuccess,
}: ProductModalFormProps) {
  const [form] = Form.useForm()

  useEffect(() => {
    if (!open) return
    if (mode === 'edit' && initial) {
      form.setFieldsValue({
        name: initial.name,
        description: initial.description,
        price: initial.price,
        stock: initial.stock,
        imageUrl: initial.imageUrl,
        isAvailable: initial.isAvailable,
        categoryId: initial.category?.id,
      })
    } else {
      form.resetFields()
      form.setFieldsValue({ isAvailable: true })
    }
  }, [open, mode, initial, form])

  const submit = async () => {
    const v = await form.validateFields()
    if (mode === 'create') {
      await http.post(
        '/api/products',
        {
          name: v.name,
          description: v.description,
          price: v.price,
          stock: v.stock,
          imageUrl: v.imageUrl,
        },
        { params: { categoryId: v.categoryId } },
      )
    } else if (initial) {
      await http.put(`/api/products/${initial.id}`, {
        name: v.name,
        description: v.description,
        price: v.price,
        stock: v.stock,
        imageUrl: v.imageUrl,
        isAvailable: v.isAvailable,
        category: { id: v.categoryId },
      })
    }
    onSuccess()
    onClose()
  }

  return (
    <Modal
      open={open}
      title={mode === 'create' ? '新建商品' : '编辑商品'}
      onCancel={onClose}
      onOk={submit}
      destroyOnClose
      width={560}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="categoryId" label="分类" rules={[{ required: true }]}>
          <Select
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
            placeholder="选择分类"
          />
        </Form.Item>
        <Form.Item name="price" label="价格" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="stock" label="库存" rules={[{ required: true }]}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="imageUrl" label="图片 URL">
          <Input placeholder="https://..." />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} />
        </Form.Item>
        {mode === 'edit' ? (
          <Form.Item name="isAvailable" label="上架" valuePropName="checked">
            <Switch />
          </Form.Item>
        ) : null}
      </Form>
    </Modal>
  )
}
