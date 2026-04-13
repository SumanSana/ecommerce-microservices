export class Constants {
    public static readonly BASE_URL = 'http://localhost:8080/ekart/v1';
    public static readonly AUTH_SERVICE_URL = `${Constants.BASE_URL}/auth`;
    public static readonly IDENTITY_SERVICE_URL = `${Constants.BASE_URL}/identity`
    public static readonly PRODUCT_SERVICE_URL = `${Constants.BASE_URL}/products`;
    public static readonly PRODUCT_MANGEMENT_SERVICE_URL = `${Constants.PRODUCT_SERVICE_URL}/admin`;
    public static readonly INVENTORY_SERVICE_URL = `${Constants.BASE_URL}/inventory/admin`;
    public static readonly ORDER_SERVICE_URL = `${Constants.BASE_URL}/orders`;
}