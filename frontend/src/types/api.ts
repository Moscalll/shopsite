export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface ApiMessageResponse {
  message: string
}

export interface UserMe {
  id: number
  username: string
  role: string
}

export interface Category {
  id: number
  name: string
  description?: string
  imageUrl?: string
}

export interface Product {
  id: number
  name: string
  description?: string
  price: number
  stock: number
  isAvailable: boolean
  imageUrl?: string
  category?: Category
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
