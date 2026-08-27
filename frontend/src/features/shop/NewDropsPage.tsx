import { ProductListingPage } from './ProductListingPage'

/**
 * Every ACTIVE product across every category, newest first — the dedicated
 * page behind the homepage's "New drops" tab and its "Shop new drops"
 * button. Deliberately not in CATEGORY_NAV, so it never appears in the
 * navbar; it's only reachable from the homepage.
 */
export function NewDropsPage() {
  return (
    <ProductListingPage
      heading="New Drops"
      status="ACTIVE"
      sortNewestFirst
      emptyMessage="No new drops yet — check back soon."
    />
  )
}
