/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

interface UseNumberShortcutsProps<T> {
  items?: T[];
  getPath?: (item: T) => string | undefined | null;
  enableFieldJumps?: boolean;
}

export default function useNumberShortcuts<T = unknown>({
  items,
  getPath,
  enableFieldJumps,
}: UseNumberShortcutsProps<T> = {}) {
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.defaultPrevented) {
        return;
      }

      // Allow Escape key to blur any active input
      if (event.key === "Escape") {
        if (document.activeElement instanceof HTMLElement) {
          event.preventDefault();
          document.activeElement.blur();
        }
        return;
      }

      if (!event.altKey || event.ctrlKey || event.metaKey) {
        return;
      }

      const keyString = event.code
        ? event.code.replace("Digit", "").replace("Numpad", "")
        : event.key;
      const shortcutIndex =
        keyString === "0" ? 10 : Number.parseInt(keyString, 10);

      if (
        Number.isNaN(shortcutIndex) ||
        shortcutIndex < 1 ||
        shortcutIndex > 10
      ) {
        return;
      }

      let handled = false;

      // 1. Try list items
      if (items && getPath) {
        const item = items[shortcutIndex - 1];
        if (item) {
          const path = getPath(item);
          if (path) {
            event.preventDefault();
            navigate(path);
            handled = true;
          }
        }
      }

      // 2. Try field jumps if enabled and not already handled
      if (!handled && enableFieldJumps) {
        const selector = `[data-shortcut-index="${shortcutIndex}"]`;
        const target = document.querySelector<HTMLElement>(selector);
        if (target) {
          event.preventDefault();
          target.focus({ preventScroll: true });
          target.scrollIntoView({ behavior: "smooth", block: "center" });

          // If the target is a dropdown trigger, open it automatically
          if (
            target.getAttribute("role") === "combobox" ||
            target.getAttribute("data-slot") === "select-trigger" ||
            target.tagName.toLowerCase() === "button"
          ) {
            setTimeout(() => {
              target.dispatchEvent(
                new KeyboardEvent("keydown", {
                  key: "ArrowDown",
                  code: "ArrowDown",
                  bubbles: true,
                  cancelable: true,
                }),
              );
              target.click();
            }, 10);
          }

          handled = true;
        }
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [items, getPath, enableFieldJumps, navigate]);
}
