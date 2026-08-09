import type { ReactNode } from 'react'

export interface Column<T> {
  header: string
  cell: (row: T) => ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  emptyMessage?: string
}

export function DataTable<T>({ columns, rows, rowKey, emptyMessage = 'Nothing here yet.' }: DataTableProps<T>) {
  if (rows.length === 0) {
    return <div className="border border-ep-border bg-ep-surface p-8 text-center text-sm text-ep-muted">{emptyMessage}</div>
  }

  return (
    <div className="overflow-x-auto border border-ep-border">
      <table className="w-full min-w-max text-left text-sm">
        <thead>
          <tr className="border-b border-ep-border bg-ep-surface">
            {columns.map((col) => (
              <th key={col.header} className="whitespace-nowrap px-4 py-3 text-xs uppercase tracking-widest text-ep-muted">
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)} className="border-b border-ep-border last:border-0 hover:bg-ep-surface">
              {columns.map((col) => (
                <td key={col.header} className={`whitespace-nowrap px-4 py-3 ${col.className ?? ''}`}>
                  {col.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
