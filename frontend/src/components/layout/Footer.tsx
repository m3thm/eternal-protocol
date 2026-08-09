import { Link } from 'react-router-dom'
import { FaInstagram, FaTiktok, FaWhatsapp } from 'react-icons/fa6'

const socials = [
  { label: 'Instagram', href: 'https://www.instagram.com/eternal.protocol?igsh=eW1pdmF2cG55MHZx&utm_source=qr', icon: FaInstagram },
  { label: 'TikTok', href: 'https://www.tiktok.com/@eternal.protocol?_r=1&_t=ZN-96P4ZJFjX2k', icon: FaTiktok },
  { label: 'WhatsApp', href: 'https://wa.me/94760654096/', icon: FaWhatsapp },
]

export function Footer() {
  return (
    <footer className="border-t border-ep-border">
      <div className="mx-auto max-w-7xl px-4 pb-20 pt-12 sm:px-6 lg:px-8 lg:pb-28 lg:pt-16">
        <div className="flex flex-col items-center gap-12 lg:flex-row lg:items-start lg:justify-start lg:gap-20">
          <img
            src="/eternal-protocol-logo.png"
            alt="Eternal Protocol"
            className="h-40 w-40 lg:h-48 lg:w-48"
            style={{ filter: 'brightness(0) invert(1)' }}
          />

          <div className="flex flex-col items-center gap-10 sm:flex-row sm:flex-wrap sm:items-start sm:justify-center sm:gap-24 lg:items-start lg:justify-start">
            <div>
              <div className="text-sm font-semibold uppercase tracking-widest text-ep-bone">Shop</div>
              <ul className="mt-4 space-y-2 text-sm">
                <li>
                  <Link to="/shop" className="text-ep-muted hover:text-ep-bone">
                    All products
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <div className="text-sm font-semibold uppercase tracking-widest text-ep-bone">Account</div>
              <ul className="mt-4 space-y-2 text-sm">
                <li>
                  <Link to="/login" className="text-ep-muted hover:text-ep-bone">
                    Sign in
                  </Link>
                </li>
                <li>
                  <Link to="/register" className="text-ep-muted hover:text-ep-bone">
                    Sign up
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <div className="text-sm font-semibold uppercase tracking-widest text-ep-bone">Contact</div>
              <a
                href="mailto:eternalprotocol@gmail.com"
                className="mt-4 block text-sm text-ep-muted hover:text-ep-bone"
              >
                eternalprotocol@gmail.com
              </a>
            </div>

            <div>
              <div className="text-sm font-semibold uppercase tracking-widest text-ep-bone">Follow us</div>
              <div className="mt-4 flex gap-3">
                {socials.map(({ label, href, icon: Icon }) => (
                  <a
                    key={label}
                    href={href}
                    target="_blank"
                    rel="noreferrer"
                    aria-label={label}
                    className="flex h-11 w-11 items-center justify-center rounded-full border border-ep-border text-ep-bone transition-colors hover:border-ep-bone"
                  >
                    <Icon className="h-5 w-5" />
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-16 flex flex-col gap-2 border-t border-ep-border pt-6 text-xs text-ep-muted sm:flex-row sm:items-center sm:justify-between">
          <span>© {new Date().getFullYear()} Eternal Protocol. All rights reserved.</span>
          <span>Delivery fee is payable to the courier on delivery.</span>
        </div>
      </div>
    </footer>
  )
}
