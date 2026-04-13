export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
  first: boolean;
}

export interface ProductSearchCriteria {
  brandName?: string;
  categoryName?: string;
  searchTerm?: string;
  inStockOnly?: boolean;
  minPrice?: number;
  maxPrice?: number;
}

export interface ProductView {
  productId: string;
  name: string;
  description: string;
  brandName: string;
  categoryName: string;
  status: string;
  variants: ProductVariantView[];
  lastSyncedAt: string;
}

export interface ProductVariantView {
  skuId: string;
  price: number;
  availableQuantity: number;
  status: string;
  inStock: boolean;
  attributes: Record<string, any>;
  imageUrl: string;
}