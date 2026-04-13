export interface CreateProductRequest {
  name: string;
  description: string;
  brandId: string;
  categoryId: string;
  variants: VariantRequest[];
}

export interface VariantRequest {
  skuId: string;
  price: number;
  imageURl: string;
  attributes: Record<string, any>; 
}

export interface Brand{
  name: string;
}

export interface Category{
  name: string;
}