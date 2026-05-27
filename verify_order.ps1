# PowerShell integration test script for Spring Boot E-Commerce backend

$baseUrl = "http://localhost:8080/api"

# Helper for JSON post/get
function Send-Request {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [string]$Token
    )
    $headers = @{}
    if ($Token) {
        $headers.Add("Authorization", "Bearer $Token")
    }
    if ($Body) {
        $headers.Add("Content-Type", "application/json")
    }
    
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -Body $Body -ErrorAction Stop
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -ErrorAction Stop
        }
        return $response
    } catch {
        if ($_.Exception.Response) {
            $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errResponse = $streamReader.ReadToEnd()
            $streamReader.Close()
            return $errResponse | ConvertFrom-Json
        } else {
            Write-Error $_.Exception.Message
            return $null
        }
    }
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "1. Register/Login Admin and User" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Register Admin
$adminRegBody = @{
    name = "Shop Admin"
    email = "admin_phase4@example.com"
    password = "adminpassword"
    role = "ADMIN"
} | ConvertTo-Json
$res = Send-Request -Method "Post" -Url "$baseUrl/auth/register" -Body $adminRegBody
Write-Host "Admin Registration Response: $res"

# Login Admin
$adminLoginBody = @{
    email = "admin_phase4@example.com"
    password = "adminpassword"
} | ConvertTo-Json
$adminAuth = Send-Request -Method "Post" -Url "$baseUrl/auth/login" -Body $adminLoginBody
$adminToken = $adminAuth.token
Write-Host "Admin Login Token: $adminToken"

# Register User
$userRegBody = @{
    name = "Shop Customer"
    email = "user_phase4@example.com"
    password = "userpassword"
    role = "USER"
} | ConvertTo-Json
$res = Send-Request -Method "Post" -Url "$baseUrl/auth/register" -Body $userRegBody
Write-Host "User Registration Response: $res"

# Login User
$userLoginBody = @{
    email = "user_phase4@example.com"
    password = "userpassword"
} | ConvertTo-Json
$userAuth = Send-Request -Method "Post" -Url "$baseUrl/auth/login" -Body $userLoginBody
$userToken = $userAuth.token
Write-Host "User Login Token: $userToken"

# Register User 2 (for unauthorized check)
$user2RegBody = @{
    name = "Other Customer"
    email = "user2_phase4@example.com"
    password = "userpassword"
    role = "USER"
} | ConvertTo-Json
$res = Send-Request -Method "Post" -Url "$baseUrl/auth/register" -Body $user2RegBody
$user2LoginBody = @{
    email = "user2_phase4@example.com"
    password = "userpassword"
} | ConvertTo-Json
$user2Auth = Send-Request -Method "Post" -Url "$baseUrl/auth/login" -Body $user2LoginBody
$user2Token = $user2Auth.token

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "2. Admin creates a Product" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$productBody = @{
    name = "iPhone 15"
    description = "Apple iPhone 15 with 128GB"
    price = 79999.00
    stockQuantity = 10
    category = "Mobile"
    imageUrl = "https://example.com/iphone15.jpg"
} | ConvertTo-Json
$product = Send-Request -Method "Post" -Url "$baseUrl/products" -Body $productBody -Token $adminToken
$productId = $product.id
Write-Host "Created Product ID: $productId, Name: $($product.name), Stock: $($product.stockQuantity)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "3. User adds the product to cart" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$cartAddBody = @{
    productId = $productId
    quantity = 2
} | ConvertTo-Json
$cart = Send-Request -Method "Post" -Url "$baseUrl/cart/add" -Body $cartAddBody -Token $userToken
Write-Host "Cart after adding: Total Amount: $($cart.totalAmount), Items Count: $($cart.items.Count)"
$cartItem = $cart.items[0]
Write-Host "Cart Item ID: $($cartItem.cartItemId), Product: $($cartItem.productName), Qty: $($cartItem.quantity), Subtotal: $($cartItem.subtotal)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "4. Stock Limit Check (Adding too much quantity)" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$cartAddTooMuch = @{
    productId = $productId
    quantity = 9 # Total would be 2 + 9 = 11, which exceeds stock of 10
} | ConvertTo-Json
$err = Send-Request -Method "Post" -Url "$baseUrl/cart/add" -Body $cartAddTooMuch -Token $userToken
Write-Host "Expected Stock Error Code: $($err.status)"
Write-Host "Expected Stock Error Message: $($err.message)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "5. User places an Order" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$placeOrderBody = @{
    shippingAddress = "Siliguri, West Bengal, India"
    phoneNumber = "9876543210"
} | ConvertTo-Json
$order = Send-Request -Method "Post" -Url "$baseUrl/orders/place" -Body $placeOrderBody -Token $userToken
$orderId = $order.orderId
Write-Host "Placed Order Response: ID: $orderId, Status: $($order.status), Address: $($order.shippingAddress), Total: $($order.totalAmount)"
Write-Host "Order Items Count: $($order.items.Count)"
$orderItem = $order.items[0]
Write-Host "OrderItem: ID: $($orderItem.orderItemId), Product: $($orderItem.productName), Qty: $($orderItem.quantity), Price: $($orderItem.price)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "6. Verify Stock is Reduced and Cart is Cleared" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Fetch product stock
$updatedProduct = Send-Request -Method "Get" -Url "$baseUrl/products/$productId"
Write-Host "Product Stock now: $($updatedProduct.stockQuantity) (Expected: 8)"

# Fetch cart
$updatedCart = Send-Request -Method "Get" -Url "$baseUrl/cart" -Token $userToken
Write-Host "Cart items count: $($updatedCart.items.Count) (Expected: 0)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "7. View Order History & View Single Order" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$history = Send-Request -Method "Get" -Url "$baseUrl/orders/my-orders" -Token $userToken
Write-Host "Orders in history: $($history.Count)"

$singleOrder = Send-Request -Method "Get" -Url "$baseUrl/orders/$orderId" -Token $userToken
Write-Host "Fetched Single Order total: $($singleOrder.totalAmount)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "8. Security / Unauthorized checks" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# User 2 tries to view User 1's order
$authErr = Send-Request -Method "Get" -Url "$baseUrl/orders/$orderId" -Token $user2Token
Write-Host "User 2 viewing User 1 order - Code: $($authErr.status), Message: $($authErr.message)"

# Try placing order with empty cart
$emptyCartErr = Send-Request -Method "Post" -Url "$baseUrl/orders/place" -Body $placeOrderBody -Token $userToken
Write-Host "Place order with empty cart - Code: $($emptyCartErr.status), Message: $($emptyCartErr.message)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Verification Complete" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
