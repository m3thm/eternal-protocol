import { ProductListingPage } from './ProductListingPage'

/**
 * Every COMING_SOON product across every category, newest first — the
 * dedicated page behind the homepage's "Upcoming" tab and its "View
 * upcoming" button. The only place these products are browsable at all;
 * they never appear on the regular category pages or New Drops, and can't
 * be added to a cart or ordered (enforced server-side, not just here).
 * Deliberately not in CATEGORY_NAV, so it never appears in the navbar.
 */
export function UpcomingPage() {
  return (
    <ProductListingPage
      heading="Upcoming"
      status="COMING_SOON"
      sortNewestFirst
      emptyMessage="Nothing coming soon right now — check back later."
    />
  )
}
