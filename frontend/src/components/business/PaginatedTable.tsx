import type { TableColumnsType, TablePaginationConfig } from 'antd'
import { Table } from 'antd'

interface PaginatedTableProps<T> {
  columns: TableColumnsType<T>
  dataSource: T[]
  loading: boolean
  rowKey: keyof T | ((record: T) => string | number)
  pagination: TablePaginationConfig
}

export function PaginatedTable<T extends object>({
  columns,
  dataSource,
  loading,
  rowKey,
  pagination,
}: PaginatedTableProps<T>) {
  return (
    <Table<T>
      columns={columns}
      dataSource={dataSource}
      loading={loading}
      rowKey={rowKey}
      pagination={pagination}
      scroll={{ x: 'max-content' }}
    />
  )
}
