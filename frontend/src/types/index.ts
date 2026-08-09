// Mirrors BACKEND_API_SPEC.md exactly. Keep this in sync when the backend changes.

export type Role = 'CUSTOMER' | 'ATHLETE' | 'ADMIN'

export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED'
export type CommissionStatus = 'AWAITING_PAYMENT' | 'EARNED' | 'PAID'

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}

// ---- Module 1: Storefront & Catalog ----

export interface ProductDto {
  id: number
  styleCode: string
  name: string
  price: number
  colour: string
  size: string
  stockQty: number
  imageUrl: string | null
  description: string | null
}

export interface ProductInput {
  styleCode: string
  name: string
  price: number
  colour: string
  size: string
  stockQty: number
  imageUrl?: string
  description?: string
}

export interface UploadImageResponse {
  imageUrl: string
}

// ---- Module 2: Cart, Checkout & Payment ----

export interface ValidateCodeResponse {
  valid: boolean
  code: string
  discountPercentage: number
}

export interface OrderLineItemDto {
  productId: number
  productName: string
  size: string
  colour: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface OrderItemInput {
  productId: number
  size: string
  colour: string
  quantity: number
}

export interface CreateOrderInput {
  customerName: string
  phone: string
  address: string
  items: OrderItemInput[]
  athleteCode?: string
}

export interface CreateOrderResponse {
  orderId: number
  totalAmount: number
  paymentUrl: string
}

export interface OrderDetailDto {
  id: number
  items: OrderLineItemDto[]
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  paymentStatus: PaymentStatus
  athleteCode: string | null
  orderDate: string
}

export interface CustomerProfileDto {
  id: number
  name: string
  email: string
  phone: string
  address: string
}

export interface UpdateProfileInput {
  name: string
  phone: string
  address: string
}

export interface CartItemDto {
  id: number
  productId: number
  productName: string
  price: number
  size: string
  colour: string
  quantity: number
  imageUrl: string | null
}

export interface AddCartItemInput {
  productId: number
  size: string
  colour: string
  quantity: number
}

// ---- Module 3: Athlete System ----

export interface AthleteDashboardOrder {
  orderId: number
  items: OrderLineItemDto[]
  orderDate: string
  paymentStatus: PaymentStatus
  commissionAmount: number
  commissionStatus: CommissionStatus
}

export interface AthleteDashboardDto {
  athleteCode: string
  commissionRate: number
  totalOrders: number
  salesGenerated: number
  commissionEarned: number
  paidCommission: number
  pendingCommission: number
  orders: AthleteDashboardOrder[]
}

// ---- Module 4: Admin Dashboard ----

export interface AdminOrderDto {
  id: number
  customerName: string
  customerPhone: string
  customerAddress: string
  items: OrderLineItemDto[]
  subtotalAmount: number
  totalAmount: number
  discountAmount: number
  paymentStatus: PaymentStatus
  athleteCode: string | null
  orderDate: string
}

export interface AthleteDto {
  id: number
  name: string
  email: string
  athleteCode: string
  commissionRate: number
  active: boolean
}

export interface CreateAthleteInput {
  name: string
  email: string
  password: string
  athleteCode: string
  commissionRate: number
}

export interface UpdateAthleteInput {
  commissionRate?: number
  active?: boolean
}

export interface CommissionDto {
  id: number
  athleteName: string
  athleteCode: string
  orderId: number
  commissionAmount: number
  commissionStatus: CommissionStatus
  paidDate: string | null
}

// ---- Auth ----

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  name: string
  email: string
  password: string
  phone: string
  address: string
}

export interface AuthResponse {
  token: string
  role: Role
  userId: number
}
