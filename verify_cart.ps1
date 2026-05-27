$baseUrl = "http://localhost:8080/api"

# Helper for HTTP requests
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
    
    $jsonBody = $null
    if ($Body) {
        $headers.Add("Content-Type", "application/json")
        $jsonBody = $Body
    }
    
    try {
        if ($jsonBody) {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers -Body $jsonBody -ErrorAction Stop
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
Write-Host "Test 1: View cart without token (Expected: 401)" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$err = Send-Request -Method "Get" -Url "$baseUrl/cart"
if ($null -ne $err) {
    if ($err.status) {
        Write-Host "Error Code: $($err.status)"
        Write-Host "Error Message: $($err.message)"
    } else {
        Write-Host "Returned response: $err"
    }
} else {
    Write-Host "Returned null (unauthorized successfully blocked by Spring Security)"
}

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 2: Register & Login USER" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Register a fresh User
$userRegBody = @{
    name = "Cart User"
    email = "user_cart_test@example.com"
    password = "userpassword"
    role = "USER"
} | ConvertTo-Json
$regRes = Send-Request -Method "Post" -Url "$baseUrl/auth/register" -Body $userRegBody

# Login User
$userLoginBody = @{
    email = "user_cart_test@example.com"
    password = "userpassword"
} | ConvertTo-Json
$userAuth = Send-Request -Method "Post" -Url "$baseUrl/auth/login" -Body $userLoginBody
$userToken = $userAuth.token
if ($userToken) {
    Write-Host "User JWT: $($userToken.Substring(0, 20))..."
} else {
    Write-Host "Failed to get USER token!"
}

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 3: Add product to cart" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Let's get product ID (using product ID 1 or 2 which we created earlier)
$products = Send-Request -Method "Get" -Url "$baseUrl/products"
$productId = $products[0].id
Write-Host "Target Product ID: $productId, Name: $($products[0].name), Stock: $($products[0].stockQuantity)"

$cartAddBody = @{
    productId = $productId
    quantity = 2
} | ConvertTo-Json
$cart = Send-Request -Method "Post" -Url "$baseUrl/cart/add" -Body $cartAddBody -Token $userToken
Write-Host "Cart totalAmount: $($cart.totalAmount), Items Count: $($cart.items.Count)"
$cartItem = $cart.items[0]
$cartItemId = $cartItem.cartItemId
Write-Host "Added item: ID: $cartItemId, Price: $($cartItem.price), Qty: $($cartItem.quantity), Subtotal: $($cartItem.subtotal)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 4: View user's cart" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$viewCart = Send-Request -Method "Get" -Url "$baseUrl/cart" -Token $userToken
Write-Host "Fetched Cart Total: $($viewCart.totalAmount), Total Items: $($viewCart.totalItems)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 5: Update cart item quantity" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$updateBody = @{
    quantity = 3
} | ConvertTo-Json
$updatedCart = Send-Request -Method "Put" -Url "$baseUrl/cart/item/$cartItemId" -Body $updateBody -Token $userToken
$updatedItem = $updatedCart.items[0]
Write-Host "Updated Item Qty: $($updatedItem.quantity), Subtotal: $($updatedItem.subtotal), Cart Total: $($updatedCart.totalAmount)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 6: Remove cart item" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$removeRes = Send-Request -Method "Delete" -Url "$baseUrl/cart/item/$cartItemId" -Token $userToken
Write-Host "Remove Response returned (expecting null/empty): $removeRes"

$afterRemoveCart = Send-Request -Method "Get" -Url "$baseUrl/cart" -Token $userToken
Write-Host "Cart items count after removal: $($afterRemoveCart.items.Count)"

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "Test 7: Clear full cart" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Add it back first
$temp = Send-Request -Method "Post" -Url "$baseUrl/cart/add" -Body $cartAddBody -Token $userToken
Write-Host "Added product back to cart. Items count: $($temp.items.Count)"

# Clear cart
$clearRes = Send-Request -Method "Delete" -Url "$baseUrl/cart/clear" -Token $userToken
Write-Host "Clear Response returned (expecting null/empty): $clearRes"

$finalCart = Send-Request -Method "Get" -Url "$baseUrl/cart" -Token $userToken
Write-Host "Cart items count after clearing: $($finalCart.items.Count), Total Amount: $($finalCart.totalAmount)"
