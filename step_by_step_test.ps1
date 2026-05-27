$baseUrl = "http://localhost:8080/api"

# Unique emails for this specific run to make sure there are no register conflicts
$adminEmail = "admin_seq_" + (Get-Date -Format "yyyyMMddHHmmss") + "@example.com"
$userEmail = "user_seq_" + (Get-Date -Format "yyyyMMddHHmmss") + "@example.com"

function Display-Title {
    param([string]$Text)
    Write-Host "`n------------------------------------------------------------" -ForegroundColor Cyan
    Write-Host $Text -ForegroundColor Cyan
    Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
}

function Send-Req {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [string]$Token
    )
    $url = "$baseUrl$Path"
    Write-Host "Request: $Method $url" -ForegroundColor Gray
    
    $headers = @{}
    if ($Token) {
        $headers.Add("Authorization", "Bearer $Token")
        Write-Host "Header: Authorization: Bearer ...$(SubString-Token $Token)" -ForegroundColor Gray
    }
    
    $jsonBody = $null
    if ($Body) {
        $jsonBody = $Body | ConvertTo-Json
        Write-Host "Body: $jsonBody" -ForegroundColor Gray
    }
    
    try {
        if ($jsonBody) {
            $headers.Add("Content-Type", "application/json")
            $res = Invoke-RestMethod -Uri $url -Method $Method -Headers $headers -Body $jsonBody -ErrorAction Stop
        } else {
            $res = Invoke-RestMethod -Uri $url -Method $Method -Headers $headers -ErrorAction Stop
        }
        Write-Host "Response Status: OK (200/201)" -ForegroundColor Green
        Write-Host "Response Body:" -ForegroundColor DarkGreen
        $res | ConvertTo-Json -Depth 5 | Out-Host
        return $res
    } catch {
        Write-Host "Response Status: ERROR ($($_.Exception.Response.StatusCode.value__))" -ForegroundColor Red
        if ($_.Exception.Response) {
            $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errResponse = $streamReader.ReadToEnd()
            $streamReader.Close()
            Write-Host "Response Body:" -ForegroundColor DarkRed
            $errResponse
        } else {
            Write-Host "Error details: $_" -ForegroundColor Red
        }
        return $null
    }
}

function SubString-Token {
    param([string]$token)
    if ($token.Length -gt 15) {
        return $token.Substring(0, 15) + "..."
    }
    return $token
}

# Start flow:

# 1. Register/login ADMIN
Display-Title "1. Register & Login ADMIN"
$regAdminBody = @{
    name = "Sequence Admin"
    email = $adminEmail
    password = "adminpassword"
    role = "ADMIN"
}
$regAdminRes = Send-Req -Method "Post" -Path "/auth/register" -Body $regAdminBody

$loginAdminBody = @{
    email = $adminEmail
    password = "adminpassword"
}
$loginAdminRes = Send-Req -Method "Post" -Path "/auth/login" -Body $loginAdminBody
$adminToken = $loginAdminRes.token

# 2. Register/login USER
Display-Title "2. Register & Login USER"
$regUserBody = @{
    name = "Sequence User"
    email = $userEmail
    password = "userpassword"
    role = "USER"
}
$regUserRes = Send-Req -Method "Post" -Path "/auth/register" -Body $regUserBody

$loginUserBody = @{
    email = $userEmail
    password = "userpassword"
}
$loginUserRes = Send-Req -Method "Post" -Path "/auth/login" -Body $loginUserBody
$userToken = $loginUserRes.token

# 3. Create product using ADMIN token
Display-Title "3. Create Product using ADMIN Token"
$productBody = @{
    name = "PlayStation 5"
    description = "Sony PlayStation 5 Console"
    price = 54999.00
    stockQuantity = 5
    category = "Gaming"
    imageUrl = "https://example.com/ps5.jpg"
}
$productRes = Send-Req -Method "Post" -Path "/products" -Body $productBody -Token $adminToken
$productId = $productRes.id

# 4. Add product to cart using USER token
Display-Title "4. Add Product to Cart using USER Token"
$cartAddBody = @{
    productId = $productId
    quantity = 1
}
$cartAddRes = Send-Req -Method "Post" -Path "/cart/add" -Body $cartAddBody -Token $userToken

# 5. View cart using USER token
Display-Title "5. View Cart using USER Token"
$viewCartRes = Send-Req -Method "Get" -Path "/cart" -Token $userToken

# 6. Place order using USER token
Display-Title "6. Place Order using USER Token"
$orderBody = @{
    shippingAddress = "Kolkata, West Bengal, India"
    phoneNumber = "9876543210"
}
$orderRes = Send-Req -Method "Post" -Path "/orders/place" -Body $orderBody -Token $userToken

# 7. Check my-orders using USER token
Display-Title "7. Check my-orders using USER Token"
$myOrdersRes = Send-Req -Method "Get" -Path "/orders/my-orders" -Token $userToken

# 8. Check cart is empty
Display-Title "8. Check Cart is Empty"
$finalCartRes = Send-Req -Method "Get" -Path "/cart" -Token $userToken

# 9. Check product stock reduced
Display-Title "9. Check Product Stock is Reduced (Expected: 4)"
$finalProductRes = Send-Req -Method "Get" -Path "/products/$productId"
