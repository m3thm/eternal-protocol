import { Route, Routes, Outlet } from 'react-router-dom'
import { Navbar } from '@/components/layout/Navbar'
import { Footer } from '@/components/layout/Footer'
import { AdminNav } from '@/components/layout/AdminNav'
import { ToastViewport } from '@/components/ui/ToastViewport'

import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { ConfirmationPage } from '@/pages/ConfirmationPage'

import { ShopPage } from '@/features/shop/ShopPage'
import { NewDropsPage } from '@/features/shop/NewDropsPage'
import { UpcomingPage } from '@/features/shop/UpcomingPage'
import { ProductDetailPage } from '@/features/shop/ProductDetailPage'
import { CartPage } from '@/features/cart/CartPage'
import { CheckoutPage } from '@/features/checkout/CheckoutPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { ProfilePage } from '@/features/account/ProfilePage'
import { OrderHistoryPage } from '@/features/account/OrderHistoryPage'
import { AthleteDashboardPage } from '@/features/athlete/AthleteDashboardPage'
import { AdminProductsPage } from '@/features/admin/AdminProductsPage'
import { AdminOrdersPage } from '@/features/admin/AdminOrdersPage'
import { AdminAthletesPage } from '@/features/admin/AdminAthletesPage'
import { AdminCommissionsPage } from '@/features/admin/AdminCommissionsPage'

import { RequireRole } from '@/routes/RequireRole'

export default function App() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/shop" element={<ShopPage />} />
          {/* Not in CATEGORY_NAV — reachable only from the homepage, never shown in the navbar. */}
          <Route path="/shop/new-drops" element={<NewDropsPage />} />
          <Route path="/shop/upcoming" element={<UpcomingPage />} />
          <Route path="/shop/:categorySlug" element={<ShopPage />} />
          <Route path="/product/:id" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order/:id/confirmation" element={<ConfirmationPage />} />

          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            path="/account"
            element={
              <RequireRole role="CUSTOMER">
                <ProfilePage />
              </RequireRole>
            }
          />
          <Route
            path="/account/orders"
            element={
              <RequireRole role="CUSTOMER">
                <OrderHistoryPage />
              </RequireRole>
            }
          />

          <Route
            path="/athlete/dashboard"
            element={
              <RequireRole role="ATHLETE">
                <AthleteDashboardPage />
              </RequireRole>
            }
          />

          <Route
            element={
              <RequireRole role="ADMIN">
                <div>
                  <AdminNav />
                  <Outlet />
                </div>
              </RequireRole>
            }
          >
            <Route path="/admin/products" element={<AdminProductsPage />} />
            <Route path="/admin/orders" element={<AdminOrdersPage />} />
            <Route path="/admin/athletes" element={<AdminAthletesPage />} />
            <Route path="/admin/commissions" element={<AdminCommissionsPage />} />
          </Route>

          <Route path="/403" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <Footer />
      <ToastViewport />
    </div>
  )
}
