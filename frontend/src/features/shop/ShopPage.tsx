import { useParams } from 'react-router-dom'
import { categoryFromSlug, labelForCategory } from '@/lib/categories'
import { ProductListingPage } from './ProductListingPage'

export function ShopPage() {
  const { categorySlug } = useParams()

  // An unrecognized slug (typo'd URL, old bookmark) just falls back to no
  // category filter rather than a hard 404 — the shop still works, it's
  // just unscoped, which is a softer failure than an error page.
  const category = categoryFromSlug(categorySlug)
  const heading = labelForCategory(category)

  // Only ACTIVE products ever show in the regular category pages — a
  // COMING_SOON product stays exclusively on the Upcoming page until an
  // admin launches it.
  return <ProductListingPage heading={heading} category={category} status="ACTIVE" />
}
