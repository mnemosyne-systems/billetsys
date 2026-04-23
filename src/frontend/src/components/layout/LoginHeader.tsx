/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";
import { MoonIcon, SunIcon } from "lucide-react";
import { useDarkModeToggle } from "../../hooks/useDarkModeToggle";

export default function LoginHeader() {
  const [now, setNow] = useState(() => new Date());
  const { darkModeToggleRef, toggleDarkMode } = useDarkModeToggle();

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  return (
    <div className="flex items-center gap-3">
      <button
        ref={darkModeToggleRef}
        type="button"
        onClick={toggleDarkMode}
        aria-pressed="false"
        className="group relative inline-flex h-8 w-16 items-center rounded-full border border-[var(--color-buttons-bg)] bg-[var(--color-buttons-bg)] p-1 text-[var(--color-buttons-text)] transition-opacity hover:bg-[var(--color-buttons-hover)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-buttons-bg)] focus-visible:ring-offset-2"
        aria-label="Toggle dark mode"
        title="Toggle dark mode"
      >
        <SunIcon className="absolute left-2 size-3.5 opacity-90" />
        <MoonIcon className="absolute right-2 size-3.5 opacity-90" />
        <span className="pointer-events-none inline-block h-6 w-6 translate-x-0 rounded-full bg-[var(--color-buttons-text)] shadow-sm transition-transform duration-200 ease-out group-aria-[pressed=true]:translate-x-8" />
      </button>
      <span className="font-semibold tabular-nums text-foreground">
        {now.toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        })}
      </span>
    </div>
  );
}
