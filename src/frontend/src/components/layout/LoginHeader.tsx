/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";

interface LoginHeaderProps {
  brandName: string;
}

export default function LoginHeader({ brandName }: LoginHeaderProps) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  return (
    <header className="bg-header-bg text-header-text px-5 py-3 flex items-center justify-between gap-4">
      <a
        className="flex items-center gap-2.5 text-header-text no-underline text-xl font-bold"
        href="/"
      >
        <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
          className="w-6 h-6 fill-none stroke-current stroke-2"
        >
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <path d="M7 8h10M7 12h10M7 16h6" />
          <path d="M6 8l1 1 2-2" />
        </svg>
        {brandName}
      </a>
      <span className="font-semibold tabular-nums">
        {now.toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        })}
      </span>
    </header>
  );
}
