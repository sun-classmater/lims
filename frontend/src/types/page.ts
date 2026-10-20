export interface ColumnConfig {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  /** 点击该列单元格时弹出关联明细窗口（如项目库的检验项目明细） */
  detail?: boolean
}

export interface FieldOption {
  label: string
  value: string
}

export interface FieldConfig {
  prop: string
  label: string
  type?: 'text' | 'textarea' | 'number' | 'select'
  required?: boolean
  disabled?: boolean
  options?: FieldOption[]
  placeholder?: string
  defaultValue?: unknown
}

export type ButtonType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

export interface PageConfig {
  entity: string
  title: string
  primaryKeys: string[]
  showIndex?: boolean
  columns: ColumnConfig[]
  searchFields?: FieldConfig[]
  formFields?: FieldConfig[]
  canCreate?: boolean
  /** 右上角新增/导入按钮改为跳转到指定路由（如导入采样单），设置后不再弹新增窗口 */
  createLink?: string
  canEdit?: boolean
  canDelete?: boolean
  canUpload?: boolean
  canExport?: boolean
  showFilters?: boolean
  showKeyword?: boolean
  searchButtonText?: string
  createButtonText?: string
  createButtonType?: ButtonType
  createDialogTitle?: string
  deleteButtonText?: string
  deleteConfirmText?: string
  actionColumnLabel?: string
  defaultFilters?: Record<string, string>
}
