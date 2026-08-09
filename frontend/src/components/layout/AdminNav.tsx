import { NavLink } from 'react-router-dom'

const links = [
  { to: '/admin/products', label: 'Products' },
  { to: '/admin/orders', label: 'Orders' },
  { to: '/admin/athletes', label: 'Athletes' },
  { to: '/admin/commissions', label: 'Commissions' },
]

export function AdminNav() {
  return (
    <nav className="mx-auto flex max-w-7xl gap-6 border-b border-ep-border px-4 py-4 sm:px-6 lg:px-8">
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          className={({ isActive }) =>
            `text-xs uppercase tracking-widest ${isActive ? 'text-ep-bone' : 'text-ep-muted hover:text-ep-bone'}`
          }
        >
          {link.label}
        </NavLink>
      ))}
    </nav>
  )
}
